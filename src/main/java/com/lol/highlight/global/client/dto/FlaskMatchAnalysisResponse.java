package com.lol.highlight.global.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaskMatchAnalysisResponse {
    private String matchId;
    private String summonerName;
    private String tagLine;
    private PlayerStats playerStats;
    private GapAnalysis gapAnalysis;
    private List<KeyMoment> keyMoments;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStats {
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Double kda;
        private Integer cs;
        private Double csPerMin;
        private Integer visionScore;
        private Double damageShare;
        private Double goldShare;
        private Integer wardPlaced;
        private Integer wardDestroyed;
        private String lane;
        private String champion;
        private String tier;
        private String division;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapAnalysis {
        private Double overallScore;
        private Map<String, MetricAnalysis> metrics;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricAnalysis {
        private String category;
        private Double playerValue;
        private Double baselineValue;
        private Double gapPercentage;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyMoment {
        private Integer timestamp;
        private String type;
        private String description;
        private Double importance;
        private Map<String, Object> details;
    }
}
