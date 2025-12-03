package com.lol.highlight.domain.analysis.entity;

import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(columnDefinition = "TEXT")
    private String strengthAnalysis;

    @Column(columnDefinition = "TEXT")
    private String weaknessAnalysis;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;

    private Double impactScore;

    private Double teamFightScore;

    private Double farmingScore;

    private Double visionScore;

    private Double objectiveControlScore;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    @Column(columnDefinition = "TEXT")
    private String aiModelData;

    @Builder
    public Analysis(Match match, String strengthAnalysis, String weaknessAnalysis,
                   String improvementSuggestions, Double impactScore, Double teamFightScore,
                   Double farmingScore, Double visionScore, Double objectiveControlScore,
                   AnalysisStatus status, String aiModelData) {
        this.match = match;
        this.strengthAnalysis = strengthAnalysis;
        this.weaknessAnalysis = weaknessAnalysis;
        this.improvementSuggestions = improvementSuggestions;
        this.impactScore = impactScore;
        this.teamFightScore = teamFightScore;
        this.farmingScore = farmingScore;
        this.visionScore = visionScore;
        this.objectiveControlScore = objectiveControlScore;
        this.status = status != null ? status : AnalysisStatus.PENDING;
        this.aiModelData = aiModelData;
    }

    public void updateAnalysisData(String strengthAnalysis, String weaknessAnalysis,
                                  String improvementSuggestions, Double impactScore,
                                  Double teamFightScore, Double farmingScore,
                                  Double visionScore, Double objectiveControlScore) {
        this.strengthAnalysis = strengthAnalysis;
        this.weaknessAnalysis = weaknessAnalysis;
        this.improvementSuggestions = improvementSuggestions;
        this.impactScore = impactScore;
        this.teamFightScore = teamFightScore;
        this.farmingScore = farmingScore;
        this.visionScore = visionScore;
        this.objectiveControlScore = objectiveControlScore;
        this.status = AnalysisStatus.COMPLETED;
    }

    public void updateStatus(AnalysisStatus status) {
        this.status = status;
    }
}
