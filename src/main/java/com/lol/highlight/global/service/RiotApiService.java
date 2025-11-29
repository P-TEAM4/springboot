package com.lol.highlight.global.service;

import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
import com.lol.highlight.domain.match.dto.MatchListResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.entity.MatchStatus;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.client.RiotApiClient;
import com.lol.highlight.global.client.dto.RiotMatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Riot API 통신 서비스
 * - DB 영구 저장 전략: 조회한 모든 매치를 영구 저장
 * - 필요한 최소 정보만 저장 (timelineData 제외)
 * - 여러 사용자가 같은 경기 조회 시 DB에서 바로 반환 (Riot API 호출 1회만)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiotApiService {

    private final RiotApiClient riotApiClient;
    private final MatchRepository matchRepository;
    private final AnalysisRepository analysisRepository;
    private final HighlightRepository highlightRepository;

    /**
     * 최근 매치 리스트 조회 (DB 우선, 없으면 Riot API 호출 후 영구 저장)
     *
     * @param user 사용자
     * @param count 조회할 매치 개수
     * @return 매치 리스트 (상세 통계 포함)
     */
    @Transactional
    public List<MatchListResponse> getRecentMatchList(User user, int count) {
        try {
            if (user.getRiotPuuid() == null) {
                log.warn("User {} has no Riot PUUID", user.getId());
                return new ArrayList<>();
            }

            log.info("Fetching recent {} matches for user: {}", count, user.getId());

            // 1. Riot API에서 매치 ID 리스트 조회
            List<String> matchIds = riotApiClient.getRecentMatchIds(user.getRiotPuuid(), count);
            List<MatchListResponse> responses = new ArrayList<>();

            // 2. 각 매치 처리 (DB 우선, 없으면 Riot API)
            for (String matchId : matchIds) {
                try {
                    MatchListResponse response = getOrFetchMatch(user, matchId);
                    if (response != null) {
                        responses.add(response);
                    }
                } catch (Exception e) {
                    log.error("Failed to process match: {}", matchId, e);
                    // 개별 매치 실패는 무시하고 계속 진행
                }
            }

            log.info("Fetched {} recent matches for user: {}", responses.size(), user.getId());
            return responses;

        } catch (Exception e) {
            log.error("Error fetching recent match list for user: {}", user.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 매치 조회 또는 가져오기 (DB 우선)
     * - DB에 있으면 DB에서 반환
     * - 없으면 Riot API 호출 후 DB에 영구 저장
     */
    private MatchListResponse getOrFetchMatch(User user, String matchId) {
        // 1. DB에서 먼저 찾기
        Match match = matchRepository.findByMatchId(matchId).orElse(null);

        if (match != null) {
            // DB에 있으면 Riot API 호출 스킵
            log.debug("Match found in DB: {}", matchId);

            boolean hasAnalysis = analysisRepository.existsByMatchId(match.getId());
            boolean hasHighlight = highlightRepository.existsByMatchId(match.getId());

            return MatchListResponse.builder()
                    .matchId(match.getMatchId())
                    .championName(match.getChampionName())
                    .kills(match.getKills())
                    .deaths(match.getDeaths())
                    .assists(match.getAssists())
                    .kda(match.getKda())
                    .win(match.getWin())
                    .gameDuration(match.getGameDuration())
                    .gameCreation(match.getGameCreation())
                    .hasAnalysis(hasAnalysis)
                    .hasHighlight(hasHighlight)
                    .build();
        }

        // 2. DB에 없으면 Riot API 호출
        log.debug("Match not in DB, fetching from Riot API: {}", matchId);
        RiotMatchResponse matchDetail = riotApiClient.getMatchDetails(matchId);
        if (matchDetail == null) {
            return null;
        }

        // 3. 참가자 정보 찾기
        RiotMatchResponse.Participant participant = findParticipant(matchDetail, user);
        if (participant == null) {
            return null;
        }

        // 4. KDA 계산
        double kda = participant.getDeaths() == 0
                ? (participant.getKills() + participant.getAssists()) * 1.0
                : (participant.getKills() + participant.getAssists()) / (participant.getDeaths() * 1.0);

        // 5. DB에 영구 저장
        Match newMatch = Match.builder()
                .user(user)
                .matchId(matchId)
                .championName(participant.getChampionName())
                .kills(participant.getKills())
                .deaths(participant.getDeaths())
                .assists(participant.getAssists())
                .kda(Math.round(kda * 100.0) / 100.0)
                .win(participant.getWin())
                .gameDuration(matchDetail.getGameDuration().intValue())
                .gameCreation(matchDetail.getGameCreation())
                .status(MatchStatus.COMPLETED)
                .build();

        matchRepository.save(newMatch);
        log.info("Match saved to DB (permanent): {}", matchId);

        // 6. MatchListResponse 생성
        return MatchListResponse.from(
                matchDetail,
                participant,
                false,  // 새로 저장된 매치는 분석 없음
                false   // 하이라이트도 없음
        );
    }

    /**
     * 분석/하이라이트 생성 시 호출: Match 조회 또는 생성
     * - DB에 있으면 재사용
     * - 없으면 Riot API에서 가져와 저장
     *
     * @param user 사용자
     * @param matchId 매치 ID
     * @return DB에 저장된 Match 엔티티
     */
    @Transactional
    public Match findOrCreateMatch(User user, String matchId) {
        return matchRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    // DB에 없으면 Riot API에서 가져와 저장
                    try {
                        RiotMatchResponse response = riotApiClient.getMatchDetails(matchId);
                        if (response == null) {
                            throw new RuntimeException("Failed to fetch match from Riot API");
                        }

                        RiotMatchResponse.Participant participant = findParticipant(response, user);
                        if (participant == null) {
                            throw new RuntimeException("Participant not found in match");
                        }

                        // KDA 계산
                        double kda = participant.getDeaths() == 0
                                ? (participant.getKills() + participant.getAssists()) * 1.0
                                : (participant.getKills() + participant.getAssists()) / (participant.getDeaths() * 1.0);

                        Match match = Match.builder()
                                .user(user)
                                .matchId(matchId)
                                .championName(participant.getChampionName())
                                .kills(participant.getKills())
                                .deaths(participant.getDeaths())
                                .assists(participant.getAssists())
                                .kda(Math.round(kda * 100.0) / 100.0)
                                .win(participant.getWin())
                                .gameDuration(response.getGameDuration().intValue())
                                .gameCreation(response.getGameCreation())
                                .status(MatchStatus.COMPLETED)
                                .build();

                        match = matchRepository.save(match);
                        log.info("Match saved to DB: {}", matchId);
                        return match;

                    } catch (Exception e) {
                        log.error("Error creating match: {}", matchId, e);
                        throw new RuntimeException("Failed to create match", e);
                    }
                });
    }

    /**
     * 매치 응답에서 해당 유저의 참가자 정보 찾기
     */
    private RiotMatchResponse.Participant findParticipant(RiotMatchResponse response, User user) {
        if (response.getParticipants() == null) {
            return null;
        }

        return response.getParticipants().stream()
                .filter(p -> user.getRiotPuuid() != null && user.getRiotPuuid().equals(p.getPuuid()))
                .findFirst()
                .orElse(null);
    }
}
