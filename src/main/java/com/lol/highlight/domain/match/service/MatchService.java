package com.lol.highlight.domain.match.service;

import com.lol.highlight.domain.match.config.MatchRefreshProperties;
import com.lol.highlight.domain.match.dto.LeagueInfoDto;
import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.dto.MatchesWithProfileResponse;
import com.lol.highlight.domain.match.dto.SummonerProfileResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.external.riot.client.RiotApiClient;
import com.lol.highlight.global.external.riot.dto.RiotLeagueDto;
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
    public MatchesWithProfileResponse getMatchesBySummonerName(
            Long requestUserId,
            String gameName,
            String tagLine,
            Pageable pageable) {

        // 1. 조회하는 회원 확인
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 조회 대상의 puuid 가져오기
        RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(gameName, tagLine);
        String targetPuuid = accountDto.getPuuid();

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

        // 6. 프로필 정보 조회
        SummonerProfileResponse profile = getSummonerProfile(gameName, tagLine);

        // 7. 통합 응답 생성
        return MatchesWithProfileResponse.builder()
                .profile(profile)
                .matches(matches.map(MatchResponse::from))
                .build();
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

        // 4. 현재 DB에 있는 전적 개수 확인
        long currentMatchCount = matchRepository.countByPuuid(targetPuuid);

        // 5. DB에 있는 전적 이후부터 추가로 가져오기
        loadMoreMatches(targetPuuid, (int) currentMatchCount);

        // 6. Rate Limit 기록
        requestUser.recordRefresh(refreshProperties.getWindowMinutes());
        requestUser.updateLastActivityAt();
        userRepository.save(requestUser);

        log.info("Loaded more matches for puuid: {} (starting from index: {}) by user: {}", targetPuuid, currentMatchCount, requestUserId);
    }

    @Transactional
    public void loadMoreMatchesFromIndex(Long requestUserId, String gameName, String tagLine, int startIndex) {
        // 1. 사용자 확인
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

        // 3. PUUID 조회
        RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(gameName, tagLine);
        String targetPuuid = accountDto.getPuuid();

        // 4. 추가 전적 로드
        loadMoreMatches(targetPuuid, startIndex);

        // 5. Rate Limit 기록
        requestUser.recordRefresh(refreshProperties.getWindowMinutes());
        requestUser.updateLastActivityAt();
        userRepository.save(requestUser);

        log.info("Loaded more matches from index {} for puuid: {} by user: {}", startIndex, targetPuuid, requestUserId);
    }

    @Transactional
    public MatchDetailResponse getMatchDetail(String matchId) {
        // 1. DB에서 Match 조회
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND, 
                            "매치를 찾을 수 없습니다: " + matchId));

        // 2. detailDataUrl 존재 확인
        if (match.getDetailDataUrl() == null || match.getDetailDataUrl().isEmpty()) {
            log.warn("Match {} has no detailDataUrl, fetching from Riot API", matchId);
            return fetchAndStoreMatchDetail(match);
        }

        // 3. Cloud Storage에서 다운로드 시도
        try {
            com.lol.highlight.global.dto.CloudStorageResponse storageResponse =
                    cloudStorageService.downloadMatchData(match.getDetailDataUrl());
            return convertToMatchDetailResponse(storageResponse);
            
        } catch (Exception e) {
            // 4. 파일 없으면 Riot API에서 재조회
            log.warn("Cloud storage file missing for matchId={}, re-fetching from Riot API. Error: {}", 
                     matchId, e.getMessage());
            return fetchAndStoreMatchDetail(match);
        }
    }

    /**
     * Riot API에서 매치 상세 정보를 가져와 Cloud Storage에 저장
     */
    private MatchDetailResponse fetchAndStoreMatchDetail(Match match) {
        try {
            // Riot API 호출
            RiotMatchDto riotMatch = riotApiClient.getMatchById(match.getMatchId());
            
            // MatchDetailResponse 변환
            MatchDetailResponse detailResponse = convertToMatchDetail(riotMatch);
            
            // Cloud Storage에 업로드
            String detailDataUrl = cloudStorageService.uploadMatchData(
                match.getMatchId(), 
                detailResponse
            );
            
            // DB 업데이트
            match.updateDetailDataUrl(detailDataUrl);
            matchRepository.save(match);
            
            log.info("Re-uploaded match detail to cloud storage: {}", match.getMatchId());
            
            return detailResponse;
            
        } catch (Exception e) {
            log.error("Failed to fetch and store match detail for matchId={}", match.getMatchId(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, 
                        "매치 상세 정보를 가져올 수 없습니다");
        }
    }

    @Transactional(readOnly = true)
    public SummonerProfileResponse getSummonerProfile(String gameName, String tagLine) {
        try {
            // 1. Riot ID로 계정 정보 가져오기 (puuid 얻기)
            RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(gameName, tagLine);
            String puuid = accountDto.getPuuid();

            // 2. puuid로 소환사 정보 가져오기 (레벨, 프로필 아이콘)
            RiotSummonerDto summonerDto = riotApiClient.getSummonerByPuuid(puuid);

            // 3. puuid로 리그 정보 가져오기 (티어, LP 등)
            List<RiotLeagueDto> leagues = riotApiClient.getLeagueByPuuid(puuid);

            // 4. 솔로랭크/자유랭크 분리
            LeagueInfoDto soloLeague = leagues.stream()
                    .filter(league -> "RANKED_SOLO_5x5".equals(league.getQueueType()))
                    .findFirst()
                    .map(this::convertToLeagueInfo)
                    .orElse(LeagueInfoDto.unranked("RANKED_SOLO_5x5"));

            LeagueInfoDto flexLeague = leagues.stream()
                    .filter(league -> "RANKED_FLEX_SR".equals(league.getQueueType()))
                    .findFirst()
                    .map(this::convertToLeagueInfo)
                    .orElse(LeagueInfoDto.unranked("RANKED_FLEX_SR"));

            // 5. 프로필 아이콘 URL 생성
            String currentVersion = dataDragonService.getActiveVersion();
            String profileIconUrl = String.format(
                    "https://ddragon.leagueoflegends.com/cdn/%s/img/profileicon/%d.png",
                    currentVersion,
                    summonerDto.getProfileIconId()
            );

            // 6. 최근 20경기 통계 계산
            com.lol.highlight.domain.match.dto.RecentStatsDto recentStats = calculateRecentStats(puuid);

            // 7. 응답 DTO 생성
            return SummonerProfileResponse.builder()
                    .gameName(gameName)
                    .tagLine(tagLine)
                    .summonerLevel(summonerDto.getSummonerLevel())
                    .profileIconUrl(profileIconUrl)
                    .soloLeague(soloLeague)
                    .flexLeague(flexLeague)
                    .recentStats(recentStats)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get summoner profile: {}#{}", gameName, tagLine, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "소환사 정보를 불러올 수 없습니다");
        }
    }

    private LeagueInfoDto convertToLeagueInfo(RiotLeagueDto league) {
        int totalGames = league.getWins() + league.getLosses();
        String winRate = totalGames > 0
                ? String.format("%.0f", (league.getWins() * 100.0 / totalGames))
                : "0";

        return LeagueInfoDto.builder()
                .queueType(league.getQueueType())
                .tier(league.getTier())
                .rank(league.getRank())
                .leaguePoints(league.getLeaguePoints())
                .wins(league.getWins())
                .losses(league.getLosses())
                .winRate(winRate)
                .build();
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
                .item0(playerData.getItem0())
                .item1(playerData.getItem1())
                .item2(playerData.getItem2())
                .item3(playerData.getItem3())
                .item4(playerData.getItem4())
                .item5(playerData.getItem5())
                .item6(playerData.getItem6())
                .gameVersion(riotMatch.getInfo().getGameVersion())
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

    private void loadMoreMatches(String puuid, int startIndex) {
        try {
            // startIndex부터 추가로 20개 가져오기
            List<String> matchIds = riotApiClient.getMatchIdsByPuuid(puuid, startIndex, DEFAULT_MATCH_COUNT);

            if (matchIds == null || matchIds.isEmpty()) {
                log.info("No more matches available for puuid: {} from index: {}", puuid, startIndex);
                return;
            }

            int newMatchCount = 0;
            for (String matchId : matchIds) {
                // DB에 이미 있으면 스킵
                if (matchRepository.existsByMatchId(matchId)) {
                    log.debug("Match already exists: {}", matchId);
                    continue;
                }

                try {
                    RiotMatchDto riotMatch = riotApiClient.getMatchById(matchId);
                    saveMatch(puuid, riotMatch);
                    newMatchCount++;
                } catch (Exception e) {
                    log.error("Failed to save match: {}", matchId, e);
                }
            }

            log.info("Loaded {} new matches for puuid: {} (from index: {})", newMatchCount, puuid, startIndex);
        } catch (Exception e) {
            log.error("Failed to load more matches for puuid: {} from index: {}", puuid, startIndex, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "추가 전적을 불러오는데 실패했습니다");
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
                .item0(playerData.getItem0())
                .item1(playerData.getItem1())
                .item2(playerData.getItem2())
                .item3(playerData.getItem3())
                .item4(playerData.getItem4())
                .item5(playerData.getItem5())
                .item6(playerData.getItem6())
                .gameVersion(riotMatch.getInfo().getGameVersion())
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

    private com.lol.highlight.domain.match.dto.RecentStatsDto calculateRecentStats(String puuid) {
        // 최근 20경기 조회
        Pageable pageable = PageRequest.of(0, 20);
        Page<Match> recentMatches = matchRepository.findByPuuidOrderByGameCreationDesc(puuid, pageable);
        List<Match> matches = recentMatches.getContent();

        if (matches.isEmpty()) {
            // 경기 데이터가 없으면 null 반환
            return null;
        }

        int totalGames = matches.size();
        int wins = 0;
        int totalKills = 0;
        int totalDeaths = 0;
        int totalAssists = 0;

        for (Match match : matches) {
            if (Boolean.TRUE.equals(match.getWin())) {
                wins++;
            }
            totalKills += match.getKills() != null ? match.getKills() : 0;
            totalDeaths += match.getDeaths() != null ? match.getDeaths() : 0;
            totalAssists += match.getAssists() != null ? match.getAssists() : 0;
        }

        int losses = totalGames - wins;
        String winRate = String.format("%.0f", (wins * 100.0 / totalGames));

        // 평균 KDA 계산
        double averageKda;
        if (totalDeaths == 0) {
            averageKda = totalKills + totalAssists;
        } else {
            averageKda = (totalKills + totalAssists) / (double) totalDeaths;
        }

        return com.lol.highlight.domain.match.dto.RecentStatsDto.builder()
                .totalGames(totalGames)
                .wins(wins)
                .losses(losses)
                .winRate(winRate)
                .averageKda(Math.round(averageKda * 100.0) / 100.0) // 소수점 2자리
                .build();
    }
}
