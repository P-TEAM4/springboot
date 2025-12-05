package com.lol.highlight.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapAnalysisRequest {

    @JsonProperty("match_id")
    private String matchId;

    private String puuid;

    private String tier;
}
