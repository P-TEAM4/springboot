package com.lol.highlight.global.external.riot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RiotSummonerDto {

    private String id;
    private String accountId;
    private String puuid;
    private String name;

    @JsonProperty("profileIconId")
    private Integer profileIconId;

    @JsonProperty("revisionDate")
    private Long revisionDate;

    @JsonProperty("summonerLevel")
    private Long summonerLevel;
}
