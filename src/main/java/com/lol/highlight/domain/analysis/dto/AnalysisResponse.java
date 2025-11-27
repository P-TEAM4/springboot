package com.lol.highlight.domain.analysis.dto;

import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.entity.AnalysisStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalysisResponse {

    private Long id;
    private Long matchId;
    private String strengthAnalysis;
    private String weaknessAnalysis;
    private String improvementSuggestions;
    private ScoreData scores;
    private AnalysisStatus status;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class ScoreData {
        private Double impactScore;
        private Double teamFightScore;
        private Double farmingScore;
        private Double visionScore;
        private Double objectiveControlScore;
        private Double averageScore;
    }

    public static AnalysisResponse from(Analysis analysis) {
        Double averageScore = calculateAverageScore(
                analysis.getImpactScore(),
                analysis.getTeamFightScore(),
                analysis.getFarmingScore(),
                analysis.getVisionScore(),
                analysis.getObjectiveControlScore()
        );

        return AnalysisResponse.builder()
                .id(analysis.getId())
                .matchId(analysis.getMatch().getId())
                .strengthAnalysis(analysis.getStrengthAnalysis())
                .weaknessAnalysis(analysis.getWeaknessAnalysis())
                .improvementSuggestions(analysis.getImprovementSuggestions())
                .scores(ScoreData.builder()
                        .impactScore(analysis.getImpactScore())
                        .teamFightScore(analysis.getTeamFightScore())
                        .farmingScore(analysis.getFarmingScore())
                        .visionScore(analysis.getVisionScore())
                        .objectiveControlScore(analysis.getObjectiveControlScore())
                        .averageScore(averageScore)
                        .build())
                .status(analysis.getStatus())
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    private static Double calculateAverageScore(Double... scores) {
        int count = 0;
        double sum = 0.0;
        for (Double score : scores) {
            if (score != null) {
                sum += score;
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }
}
