package com.lol.highlight.global.external.riot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RiotLeagueDto {

    private String queueType;
    private String tier;
    private String rank;
    private String summonerId;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
}
