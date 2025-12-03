package com.lol.highlight.domain.match.entity;

import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.enums.AuthProvider;
import com.lol.highlight.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Match 엔티티 생성 성공")
    void createMatchSuccess() {
        // given & when
        Match match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .kills(10)
                .deaths(2)
                .assists(15)
                .kda(12.5)
                .win(true)
                .gameDuration(1800)
                .gameCreation(System.currentTimeMillis())
                .status(MatchStatus.COMPLETED)
                .timelineData("{}")
                .build();

        // then
        assertThat(match.getMatchId()).isEqualTo("KR_123456789");
        assertThat(match.getChampionName()).isEqualTo("Ahri");
        assertThat(match.getKills()).isEqualTo(10);
        assertThat(match.getDeaths()).isEqualTo(2);
        assertThat(match.getAssists()).isEqualTo(15);
        assertThat(match.getKda()).isEqualTo(12.5);
        assertThat(match.getWin()).isTrue();
    }

    @Test
    @DisplayName("매치 데이터 업데이트 성공")
    void updateMatchDataSuccess() {
        // given
        Match match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .status(MatchStatus.PENDING)
                .build();

        // when
        match.updateMatchData(
                "Zed",
                15,
                5,
                8,
                4.6,
                false,
                2100,
                System.currentTimeMillis(),
                "{}"
        );

        // then
        assertThat(match.getChampionName()).isEqualTo("Zed");
        assertThat(match.getKills()).isEqualTo(15);
        assertThat(match.getDeaths()).isEqualTo(5);
        assertThat(match.getAssists()).isEqualTo(8);
        assertThat(match.getKda()).isEqualTo(4.6);
        assertThat(match.getWin()).isFalse();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("매치 상태 업데이트 성공")
    void updateStatusSuccess() {
        // given
        Match match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .status(MatchStatus.PENDING)
                .build();

        // when
        match.updateStatus(MatchStatus.COMPLETED);

        // then
        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("기본 MatchStatus는 PENDING")
    void defaultMatchStatus() {
        // given & when
        Match match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .build();

        // then
        assertThat(match.getStatus()).isEqualTo(MatchStatus.PENDING);
    }
}
