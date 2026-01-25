package com.lol.highlight.domain.match.service;

import com.lol.highlight.domain.match.config.MatchRefreshProperties;
import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.external.riot.client.RiotApiClient;
import com.lol.highlight.global.external.riot.dto.RiotMatchDto;
import com.lol.highlight.global.external.riot.dto.RiotSummonerDto;
import com.lol.highlight.global.service.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;
    private final CloudStorageService cloudStorageService;
    private final MatchRefreshProperties refreshProperties;
    private final com.lol.highlight.global.external.datadragon.DataDragonService dataDragonService;

    private static final int DEFAULT_MATCH_COUNT = 20;

    @Transactional
    public Page<MatchResponse> getMatchesBySummonerName(
            Long requestUserId,
            String gameName,
            String tagLine,
            Pageable pageable) {

        // 1. 조회하는 회원 확인
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 조회 대상의 puuid 가져오기
        RiotSummonerDto summonerDto = riotApiClient.getSummonerByRiotId(gameName, tagLine);
        String targetPuuid = summonerDto.getPuuid();

        // 3. DB에 매치가 있는지 확인
        long matchCount = matchRepository.countByPuuid(targetPuuid);

        boolean needsRefresh = false;

        if (matchCount == 0) {
            // DB에 없으면: Rate Limit 체크 후 가져오기
            if (!requestUser.canRefreshMatches(
                    refreshProperties.getMaxCount(),
                    refreshProperties.getWindowMinutes())) {
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                        String.format("요청 제한을 초과했습니다. %d분에 %d번까지 가능합니다.",
                                refreshProperties.getWindowMinutes(),
                                refreshProperties.getMaxCount()));
            }

            log.info("No matches found for puuid: {}, fetching from Riot API", targetPuuid);
            needsRefresh = true;

        } else {
            // DB에 있으면: Rate Limit 체크 후 갱신 (선택적)
            if (requestUser.canRefreshMatches(
                    refreshProperties.getMaxCount(),
                    refreshProperties.getWindowMinutes())) {
                log.info("Refreshing matches for puuid: {}", targetPuuid);
                needsRefresh = true;
            } else {
                log.info("Rate limited: User {} returning cached data", requestUserId);
            }
        }

        if (needsRefresh) {
            // Riot API 호출 및 저장
            refreshMatches(targetPuuid);

            // 최근 N개만 유지
            cleanupOldMatches(targetPuuid);

            // Rate Limit 기록
            requestUser.recordRefresh(refreshProperties.getWindowMinutes());
        }

        // 4. 활동 시간 업데이트
        requestUser.updateLastActivityAt();
        userRepository.save(requestUser);

        // 5. DB에서 조회 (정렬은 메서드 이름에 포함되어 있으므로 Pageable의 정렬 무시)
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Match> matches = matchRepository.findByPuuidOrderByGameCreationDesc(
                targetPuuid,
                unsortedPageable
        );

        return matches.map(MatchResponse::from);
    }

    @Transactional
    public void forceRefreshMatches(Long requestUserId, String gameName, String tagLine) {
        // 1. 조회하는 회원 확인
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. Rate Limit 체크
        if (!requestUser.canRefreshMatches(
                refreshProperties.getMaxCount(),
                refreshProperties.getWindowMinutes())) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    String.format("요청 제한을 초과했습니다. %d분에 %d번까지 가능합니다.",
                            refreshProperties.getWindowMinutes(),
                            refreshProperties.getMaxCount()));
        }

        // 3. puuid 가져오기
        RiotSummonerDto summonerDto = riotApiClient.getSummonerByRiotId(gameName, tagLine);
        String targetPuuid = summonerDto.getPuuid();

        // 4. 강제 갱신
        refreshMatches(targetPuuid);
        cleanupOldMatches(targetPuuid);

        // 5. Rate Limit 기록
        requestUser.recordRefresh(refreshProperties.getWindowMinutes());
        requestUser.updateLastActivityAt();
        userRepository.save(requestUser);

        log.info("Force refreshed matches for puuid: {} by user: {}", targetPuuid, requestUserId);
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetail(String matchId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (match.getDetailDataUrl() == null) {
            throw new IllegalStateException("Match detail data URL is not available");
        }

        com.lol.highlight.global.dto.CloudStorageResponse storageResponse =
                cloudStorageService.downloadMatchData(match.getDetailDataUrl());

        return convertToMatchDetailResponse(storageResponse);
    }

    private MatchDetailResponse convertToMatchDetailResponse(
            com.lol.highlight.global.dto.CloudStorageResponse storageResponse) {

        String gameVersion = storageResponse.getGameVersion();

        List<MatchDetailResponse.PlayerDetail> players = storageResponse.getPlayers().stream()
                .map(playerData -> {
                    List<MatchDetailResponse.ItemInfo> itemInfoList =
                            dataDragonService.getItemsInfo(playerData.getFinalItems(), gameVersion);

                    return MatchDetailResponse.PlayerDetail.builder()
                            .playerName(playerData.getPlayerName())
                            .championName(playerData.getChampionName())
                            .kills(playerData.getKills())
                            .deaths(playerData.getDeaths())
                            .assists(playerData.getAssists())
                            .totalDamageDealt(playerData.getTotalDamageDealt())
                            .visionScore(playerData.getVisionScore())
                            .cs(playerData.getCs())
                            .finalItems(playerData.getFinalItems())
                            .finalItemsInfo(itemInfoList)
                            .goldEarned(playerData.getGoldEarned())
                            .build();
                })
                .collect(Collectors.toList());

        List<MatchDetailResponse.TeamDetail> teams = storageResponse.getTeams().stream()
                .map(teamData -> MatchDetailResponse.TeamDetail.builder()
                        .teamId(teamData.getTeamId())
                        .win(teamData.getWin())
                        .totalObjectives(teamData.getTotalObjectives())
                        .totalKills(teamData.getTotalKills())
                        .build())
                .collect(Collectors.toList());

        return MatchDetailResponse.builder()
                .matchId(storageResponse.getMatchId())
                .gameVersion(gameVersion)
                .players(players)
                .teams(teams)
                .build();
    }

    @Transactional
    public Match fetchAndSaveMatch(String puuid, String matchId) {
        // DB에 이미 있으면 그대로 반환
        return matchRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    RiotMatchDto riotMatch = riotApiClient.getMatchById(matchId);
                    return saveAndReturnMatch(puuid, riotMatch);
                });
    }

    private Match saveAndReturnMatch(String puuid, RiotMatchDto riotMatch) {
        String matchId = riotMatch.getMetadata().getMatchId();

        MatchDetailResponse detailResponse = convertToMatchDetail(riotMatch);
        String detailDataUrl = cloudStorageService.uploadMatchData(matchId, detailResponse);

        RiotMatchDto.Participant playerData = riotMatch.getInfo().getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND, "해당 매치에서 플레이어를 찾을 수 없습니다"));

        Match match = Match.builder()
                .puuid(puuid)
                .matchId(matchId)
                .championName(playerData.getChampionName())
                .kills(playerData.getKills())
                .deaths(playerData.getDeaths())
                .assists(playerData.getAssists())
                .kda(calculateKda(playerData.getKills(), playerData.getDeaths(), playerData.getAssists()))
                .win(playerData.getWin())
                .gameDuration(riotMatch.getInfo().getGameDuration().intValue())
                .gameCreation(riotMatch.getInfo().getGameCreation())
                .status(MatchStatus.COMPLETED)
                .detailDataUrl(detailDataUrl)
                .build();

        return matchRepository.save(match);
    }

    private void refreshMatches(String puuid) {
        try {
            List<String> matchIds = riotApiClient.getMatchIdsByPuuid(puuid, DEFAULT_MATCH_COUNT);

            for (String matchId : matchIds) {
                // DB에 이미 있으면 스킵
                if (matchRepository.existsByMatchId(matchId)) {
                    log.debug("Match already exists: {}", matchId);
                    continue;
                }

                try {
                    RiotMatchDto riotMatch = riotApiClient.getMatchById(matchId);
                    saveMatch(puuid, riotMatch);
                } catch (Exception e) {
                    log.error("Failed to save match: {}", matchId, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to refresh matches for puuid: {}", puuid, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "전적 갱신에 실패했습니다");
        }
    }

    private void saveMatch(String puuid, RiotMatchDto riotMatch) {
        String matchId = riotMatch.getMetadata().getMatchId();

        // Cloud Storage에 상세 데이터 업로드
        MatchDetailResponse detailResponse = convertToMatchDetail(riotMatch);
        String detailDataUrl = cloudStorageService.uploadMatchData(matchId, detailResponse);

        // 플레이어 데이터 찾기
        RiotMatchDto.Participant playerData = riotMatch.getInfo().getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player not found in match"));

        // Match 저장
        Match match = Match.builder()
                .puuid(puuid)
                .matchId(matchId)
                .championName(playerData.getChampionName())
                .kills(playerData.getKills())
                .deaths(playerData.getDeaths())
                .assists(playerData.getAssists())
                .kda(calculateKda(playerData.getKills(), playerData.getDeaths(), playerData.getAssists()))
                .win(playerData.getWin())
                .gameDuration(riotMatch.getInfo().getGameDuration().intValue())
                .gameCreation(riotMatch.getInfo().getGameCreation())
                .status(MatchStatus.COMPLETED)
                .detailDataUrl(detailDataUrl)
                .build();

        matchRepository.save(match);
    }

    @Transactional
    public void cleanupOldMatches(String puuid) {
        matchRepository.deleteOldMatchesKeepRecent(puuid, refreshProperties.getKeepMatchCount());
        log.debug("Cleaned up old matches for puuid: {}, keeping recent {}", puuid, refreshProperties.getKeepMatchCount());
    }

    private MatchDetailResponse convertToMatchDetail(RiotMatchDto riotMatch) {
        List<MatchDetailResponse.PlayerDetail> players = riotMatch.getInfo().getParticipants().stream()
                .map(p -> {
                    List<Integer> finalItems = List.of(
                            p.getItem0(), p.getItem1(), p.getItem2(),
                            p.getItem3(), p.getItem4(), p.getItem5(), p.getItem6()
                    );

                    return MatchDetailResponse.PlayerDetail.builder()
                            .playerName(p.getSummonerName())
                            .championName(p.getChampionName())
                            .kills(p.getKills())
                            .deaths(p.getDeaths())
                            .assists(p.getAssists())
                            .totalDamageDealt(p.getTotalDamageDealtToChampions())
                            .visionScore(p.getVisionScore())
                            .cs(p.getTotalMinionsKilled() + p.getNeutralMinionsKilled())
                            .finalItems(finalItems)
                            .goldEarned(p.getGoldEarned())
                            .build();
                })
                .collect(Collectors.toList());

        List<MatchDetailResponse.TeamDetail> teams = riotMatch.getInfo().getTeams().stream()
                .map(t -> {
                    int totalObjectives = t.getObjectives().getBaron().getKills()
                            + t.getObjectives().getDragon().getKills()
                            + t.getObjectives().getTower().getKills()
                            + t.getObjectives().getInhibitor().getKills()
                            + t.getObjectives().getRiftHerald().getKills();

                    int totalKills = riotMatch.getInfo().getParticipants().stream()
                            .filter(p -> p.getTeamId().equals(t.getTeamId()))
                            .mapToInt(RiotMatchDto.Participant::getKills)
                            .sum();

                    return MatchDetailResponse.TeamDetail.builder()
                            .teamId(t.getTeamId())
                            .win(t.getWin())
                            .totalObjectives(totalObjectives)
                            .totalKills(totalKills)
                            .build();
                })
                .collect(Collectors.toList());

        return MatchDetailResponse.builder()
                .matchId(riotMatch.getMetadata().getMatchId())
                .gameVersion(riotMatch.getInfo().getGameVersion())
                .players(players)
                .teams(teams)
                .build();
    }

    private Double calculateKda(Integer kills, Integer deaths, Integer assists) {
        if (deaths == null || deaths == 0) {
            return (kills != null ? kills : 0) + (assists != null ? assists : 0) * 1.0;
        }
        return ((kills != null ? kills : 0) + (assists != null ? assists : 0)) / (double) deaths;
    }
}
