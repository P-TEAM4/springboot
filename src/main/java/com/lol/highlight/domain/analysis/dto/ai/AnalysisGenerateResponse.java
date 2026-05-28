package com.lol.highlight.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class AnalysisGenerateResponse {

    @JsonProperty("match_id")
    private String matchId;

    private String puuid;

    @JsonProperty("game_name")
    private String gameName;

    @JsonProperty("tag_line")
    private String tagLine;

    private String champion;
    private String role;
    private Boolean win;

    @JsonProperty("impact_score")
    private Double impactScore;

    @JsonProperty("baselineProba")
    private Double baselineProba;

    @JsonProperty("predictedProba")
    private Double predictedProba;

    private String summary;

    @JsonProperty("top_features")
    private List<TopFeature> topFeatures;

    @JsonProperty("gap_analysis")
    private GapAnalysisResponse gapAnalysis;

    @JsonProperty("game_duration")
    private Integer gameDuration;

    @JsonProperty("player_stats")
    private Map<String, Object> playerStats;

    @Getter
    @NoArgsConstructor
    public static class TopFeature {
        private String name;
        private String displayName;
        private String direction;
        private Double shap;
        private Double value;
    }
}
