package com.lol.highlight.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GapAnalysisResponse {

    private String tier;

    @JsonProperty("player_stats")
    private Map<String, Double> playerStats;

    @JsonProperty("tier_baseline")
    private Map<String, Double> tierBaseline;

    private Map<String, Double> gaps;

    @JsonProperty("normalized_gaps")
    private Map<String, Double> normalizedGaps;

    @JsonProperty("overall_score")
    private Double overallScore;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> recommendations;
}
