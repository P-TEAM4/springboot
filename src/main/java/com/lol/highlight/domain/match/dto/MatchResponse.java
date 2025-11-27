package com.lol.highlight.domain.match.dto;

import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.entity.MatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchResponse {

    private Long id;
    private String matchId;
    private String championName;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double kda;
    private Boolean win;
    private Integer gameDuration;
    private Long gameCreation;
    private MatchStatus status;
    private LocalDateTime createdAt;

    public static MatchResponse from(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .matchId(match.getMatchId())
                .championName(match.getChampionName())
                .kills(match.getKills())
                .deaths(match.getDeaths())
                .assists(match.getAssists())
                .kda(match.getKda())
                .win(match.getWin())
                .gameDuration(match.getGameDuration())
                .gameCreation(match.getGameCreation())
                .status(match.getStatus())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
