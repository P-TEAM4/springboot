package com.lol.highlight.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisGenerateRequest {

    @JsonProperty("match_id")
    private String matchId;

    private String puuid;

    @JsonProperty("game_name")
    private String gameName;

    @JsonProperty("tag_line")
    private String tagLine;

    private String tier;
}
