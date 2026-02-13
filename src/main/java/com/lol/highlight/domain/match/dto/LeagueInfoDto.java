package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeagueInfoDto {
    private String queueType;
    private String tier;
    private String rank;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String winRate;

    public static LeagueInfoDto unranked(String queueType) {
        return LeagueInfoDto.builder()
                .queueType(queueType)
                .tier("UNRANKED")
                .rank("")
                .leaguePoints(0)
                .wins(0)
                .losses(0)
                .winRate("0")
                .build();
    }
}
