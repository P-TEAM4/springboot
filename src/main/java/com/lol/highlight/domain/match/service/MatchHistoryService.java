package com.lol.highlight.domain.match.service;

import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.entity.MatchStatus;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.external.riot.client.RiotApiClient;
import com.lol.highlight.global.external.riot.dto.RiotLeagueDto;
import com.lol.highlight.global.external.riot.dto.RiotMatchDto;
import com.lol.highlight.global.external.riot.dto.RiotSummonerDto;
import com.lol.highlight.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;
    private final S3Service s3Service;

    private static final int DEFAULT_MATCH_COUNT = 20;

    @Transactional(readOnly = true)
    public Page<MatchResponse> getUserMatches(Long userId, Pageable pageable) {
        User user = getUserById(userId);

        user.updateLastActivityAt();
        userRepository.save(user);

        boolean needsRefresh = shouldRefreshMatches(user);

        if (needsRefresh) {
            log.info("Last activity is more than 30 minutes ago. Consider refreshing matches for user: {}", userId);
        }

        Page<Match> matches = matchRepository.findByUserId(userId, pageable);
        return matches.map(MatchResponse::from);
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetail(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        if (match.getDetailDataUrl() == null) {
            throw new IllegalStateException("Match detail data URL is not available");
        }

        MatchDetailResponse detailData = s3Service.downloadMatchData(match.getDetailDataUrl());
        return detailData;
    }

    @Transactional
    public List<MatchResponse> refreshMatches(Long userId) {
        User user = getUserById(userId);

        if (!user.canRefreshMatches()) {
            throw new IllegalStateException("Match refresh is only allowed once every 3 minutes");
        }

        log.info("Starting match refresh for user: {}", userId);

        refreshSummonerInfo(user);

        List<Match> matches = fetchAndSaveMatches(user);

        user.updateLastMatchRefreshAt();
        user.updateLastActivityAt();

        updateUserStatistics(user, matches);

        userRepository.save(user);

        log.info("Successfully refreshed {} matches for user: {}", matches.size(), userId);

        return matches.stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    private void refreshSummonerInfo(User user) {
        try {
            RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(
                    user.getSummonerName(),
                    user.getTagLine()
            );

            RiotSummonerDto summonerDto = riotApiClient.getSummonerByPuuid(accountDto.getPuuid());

            List<RiotLeagueDto> leagues = riotApiClient.getLeagueBySummonerId(summonerDto.getId());

            RiotLeagueDto rankedSolo = leagues.stream()
                    .filter(league -> "RANKED_SOLO_5x5".equals(league.getQueueType()))
                    .findFirst()
                    .orElse(null);

            if (rankedSolo != null) {
                user.updateSummonerInfo(
                        summonerDto.getProfileIconId(),
                        summonerDto.getSummonerLevel(),
                        rankedSolo.getTier(),
                        rankedSolo.getRank(),
                        rankedSolo.getLeaguePoints(),
                        rankedSolo.getWins(),
                        rankedSolo.getLosses()
                );
            } else {
                user.updateSummonerInfo(
                        summonerDto.getProfileIconId(),
                        summonerDto.getSummonerLevel(),
                        null, null, null, null, null
                );
            }

            log.info("Successfully refreshed summoner info for user: {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to refresh summoner info for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to refresh summoner info", e);
        }
    }

    private List<Match> fetchAndSaveMatches(User user) {
        try {
            RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(
                    user.getSummonerName(),
                    user.getTagLine()
            );

            List<String> matchIds = riotApiClient.getMatchIdsByPuuid(
                    accountDto.getPuuid(),
                    DEFAULT_MATCH_COUNT
            );

            List<Match> savedMatches = new ArrayList<>();

            for (String matchId : matchIds) {
                if (matchRepository.existsByMatchId(matchId)) {
                    log.debug("Match already exists, skipping: {}", matchId);
                    continue;
                }

                RiotMatchDto riotMatch = riotApiClient.getMatchById(matchId);
                Match match = convertAndSaveMatch(user, riotMatch, accountDto.getPuuid());
                savedMatches.add(match);
            }

            return savedMatches;

        } catch (Exception e) {
            log.error("Failed to fetch and save matches for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to fetch and save matches", e);
        }
    }

    private Match convertAndSaveMatch(User user, RiotMatchDto riotMatch, String puuid) {
        RiotMatchDto.Participant playerData = riotMatch.getInfo().getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player not found in match"));

        MatchDetailResponse detailResponse = convertToMatchDetail(riotMatch);

        String detailDataUrl = s3Service.uploadMatchData(
                riotMatch.getMetadata().getMatchId(),
                detailResponse
        );

        Double kda = calculateKda(playerData.getKills(), playerData.getDeaths(), playerData.getAssists());

        Match match = Match.builder()
                .user(user)
                .matchId(riotMatch.getMetadata().getMatchId())
                .championName(playerData.getChampionName())
                .kills(playerData.getKills())
                .deaths(playerData.getDeaths())
                .assists(playerData.getAssists())
                .kda(kda)
                .win(playerData.getWin())
                .gameDuration(riotMatch.getInfo().getGameDuration().intValue())
                .gameCreation(riotMatch.getInfo().getGameCreation())
                .status(MatchStatus.COMPLETED)
                .detailDataUrl(detailDataUrl)
                .build();

        return matchRepository.save(match);
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
                            .itemBuild(new ArrayList<>())
                            .skillBuild(new ArrayList<>())
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
                .players(players)
                .teams(teams)
                .build();
    }

    private void updateUserStatistics(User user, List<Match> newMatches) {
        List<Match> allMatches = matchRepository.findTop20ByUserIdOrderByGameCreationDesc(user.getId());

        if (allMatches.isEmpty()) {
            return;
        }

        double averageKda = allMatches.stream()
                .mapToDouble(m -> m.getKda() != null ? m.getKda() : 0.0)
                .average()
                .orElse(0.0);

        user.updateStatistics(averageKda, null, null);
    }

    private Double calculateKda(Integer kills, Integer deaths, Integer assists) {
        if (deaths == null || deaths == 0) {
            return (kills != null ? kills : 0) + (assists != null ? assists : 0) * 1.0;
        }
        return ((kills != null ? kills : 0) + (assists != null ? assists : 0)) / (double) deaths;
    }

    private boolean shouldRefreshMatches(User user) {
        if (user.getLastActivityAt() == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(user.getLastActivityAt().plusMinutes(30));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
