package com.lol.highlight.domain.match.dto;

import com.lol.highlight.global.client.dto.RiotMatchResponse;
import lombok.Builder;
import lombok.Getter;

/**
 * 매치 리스트 조회용 DTO (DB에 저장되지 않음)
 * Riot API에서 직접 가져온 데이터를 반환
 */
@Getter
@Builder
public class MatchListResponse {

    private String matchId;
    private String championName;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double kda;
    private Boolean win;
    private Integer gameDuration;  // 초 단위
    private Long gameCreation;  // timestamp
    private String gameMode;
    private String lane;
    private String role;
    private Integer cs;  // totalMinionsKilled
    private Integer visionScore;
    private Boolean hasAnalysis;  // DB에 분석 결과가 있는지
    private Boolean hasHighlight;  // DB에 하이라이트가 있는지

    /**
     * Riot API 응답을 MatchListResponse로 변환
     */
    public static MatchListResponse from(
            RiotMatchResponse match,
            RiotMatchResponse.Participant participant,
            Boolean hasAnalysis,
            Boolean hasHighlight
    ) {
        Double kda = participant.getDeaths() == 0
                ? (double) (participant.getKills() + participant.getAssists())
                : (double) (participant.getKills() + participant.getAssists()) / participant.getDeaths();

        return MatchListResponse.builder()
                .matchId(match.getMatchId())
                .championName(participant.getChampionName())
                .kills(participant.getKills())
                .deaths(participant.getDeaths())
                .assists(participant.getAssists())
                .kda(Math.round(kda * 100.0) / 100.0)
                .win(participant.getWin())
                .gameDuration(match.getGameDuration().intValue())
                .gameCreation(match.getGameCreation())
                .gameMode(match.getGameMode())
                .lane(participant.getLane())
                .role(participant.getRole())
                .cs(participant.getTotalMinionsKilled())
                .visionScore(participant.getVisionScore())
                .hasAnalysis(hasAnalysis)
                .hasHighlight(hasHighlight)
                .build();
    }
}
