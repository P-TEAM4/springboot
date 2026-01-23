package com.lol.highlight.global.external.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiotSummonerDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("profileIconId")
    private Integer profileIconId;

    @JsonProperty("revisionDate")
    private Long revisionDate;

    @JsonProperty("summonerLevel")
    private Long summonerLevel;
}
