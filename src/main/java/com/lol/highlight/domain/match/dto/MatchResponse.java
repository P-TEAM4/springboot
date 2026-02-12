package com.lol.highlight.domain.match.dto;

import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.enums.MatchStatus;
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
    private Integer item0;
    private Integer item1;
    private Integer item2;
    private Integer item3;
    private Integer item4;
    private Integer item5;
    private Integer item6;

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
                .item0(match.getItem0())
                .item1(match.getItem1())
                .item2(match.getItem2())
                .item3(match.getItem3())
                .item4(match.getItem4())
                .item5(match.getItem5())
                .item6(match.getItem6())
                .build();
    }
}
