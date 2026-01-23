package com.lol.highlight.global.external.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiotLeagueDto {

    private String queueType;
    private String tier;
    private String rank;
    private String summonerId;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
}
