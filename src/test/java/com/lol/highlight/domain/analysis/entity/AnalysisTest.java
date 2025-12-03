package com.lol.highlight.domain.analysis.entity;

import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.auth.enums.AuthProvider;
import com.lol.highlight.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisTest {

    private Match match;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();

        match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .status(MatchStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Analysis 엔티티 생성 성공")
    void createAnalysisSuccess() {
        // given & when
        Analysis analysis = Analysis.builder()
                .match(match)
                .strengthAnalysis("Excellent KDA and objective control")
                .weaknessAnalysis("Poor vision score in late game")
                .improvementSuggestions("Focus more on warding during mid to late game")
                .impactScore(8.5)
                .teamFightScore(9.0)
                .farmingScore(7.5)
                .visionScore(6.0)
                .objectiveControlScore(8.8)
                .status(AnalysisStatus.COMPLETED)
                .aiModelData("{}")
                .build();

        // then
        assertThat(analysis.getStrengthAnalysis()).isEqualTo("Excellent KDA and objective control");
        assertThat(analysis.getWeaknessAnalysis()).isEqualTo("Poor vision score in late game");
        assertThat(analysis.getImprovementSuggestions()).isEqualTo("Focus more on warding during mid to late game");
        assertThat(analysis.getImpactScore()).isEqualTo(8.5);
        assertThat(analysis.getTeamFightScore()).isEqualTo(9.0);
        assertThat(analysis.getFarmingScore()).isEqualTo(7.5);
        assertThat(analysis.getVisionScore()).isEqualTo(6.0);
        assertThat(analysis.getObjectiveControlScore()).isEqualTo(8.8);
    }

    @Test
    @DisplayName("분석 데이터 업데이트 성공")
    void updateAnalysisDataSuccess() {
        // given
        Analysis analysis = Analysis.builder()
                .match(match)
                .strengthAnalysis("Original strength")
                .weaknessAnalysis("Original weakness")
                .improvementSuggestions("Original suggestions")
                .status(AnalysisStatus.PENDING)
                .build();

        // when
        analysis.updateAnalysisData(
                "Updated strength analysis",
                "Updated weakness analysis",
                "Updated improvement suggestions",
                9.0,
                9.5,
                8.0,
                7.0,
                9.2
        );

        // then
        assertThat(analysis.getStrengthAnalysis()).isEqualTo("Updated strength analysis");
        assertThat(analysis.getWeaknessAnalysis()).isEqualTo("Updated weakness analysis");
        assertThat(analysis.getImprovementSuggestions()).isEqualTo("Updated improvement suggestions");
        assertThat(analysis.getImpactScore()).isEqualTo(9.0);
        assertThat(analysis.getTeamFightScore()).isEqualTo(9.5);
        assertThat(analysis.getFarmingScore()).isEqualTo(8.0);
        assertThat(analysis.getVisionScore()).isEqualTo(7.0);
        assertThat(analysis.getObjectiveControlScore()).isEqualTo(9.2);
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    }

    @Test
    @DisplayName("분석 상태 업데이트 성공")
    void updateStatusSuccess() {
        // given
        Analysis analysis = Analysis.builder()
                .match(match)
                .status(AnalysisStatus.PENDING)
                .build();

        // when
        analysis.updateStatus(AnalysisStatus.PROCESSING);

        // then
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
    }

    @Test
    @DisplayName("기본 AnalysisStatus는 PENDING")
    void defaultAnalysisStatus() {
        // given & when
        Analysis analysis = Analysis.builder()
                .match(match)
                .build();

        // then
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }
}
