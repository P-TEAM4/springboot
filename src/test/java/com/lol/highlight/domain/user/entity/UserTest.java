package com.lol.highlight.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("User 엔티티 생성 성공")
    void createUserSuccess() {
        // given & when
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .profileImage("https://example.com/profile.jpg")
                .summonerName("TestSummoner")
                .tagLine("KR1")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();

        // then
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("프로필 업데이트 성공")
    void updateProfileSuccess() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .name("Original Name")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();

        // when
        user.updateProfile("Updated Name", "https://new-image.com/profile.jpg");

        // then
        assertThat(user.getName()).isEqualTo("Updated Name");
        assertThat(user.getProfileImage()).isEqualTo("https://new-image.com/profile.jpg");
    }

    @Test
    @DisplayName("Riot 계정 연동 성공")
    void linkRiotAccountSuccess() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();

        // when
        user.linkRiotAccount("riot-puuid-123", "NewSummoner", "KR1");

        // then
        assertThat(user.getRiotPuuid()).isEqualTo("riot-puuid-123");
        assertThat(user.getSummonerName()).isEqualTo("NewSummoner");
        assertThat(user.getTagLine()).isEqualTo("KR1");
    }

    @Test
    @DisplayName("기본 UserRole은 USER")
    void defaultUserRole() {
        // given & when
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .build();

        // then
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }
}
