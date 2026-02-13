package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummonerProfileResponse {
    private String gameName;
    private String tagLine;
    private Long summonerLevel;
    private String profileIconUrl;
    private LeagueInfoDto soloLeague;
    private LeagueInfoDto flexLeague;
}
