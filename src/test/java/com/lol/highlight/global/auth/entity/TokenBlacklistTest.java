package com.lol.highlight.global.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistTest {

    @Test
    @DisplayName("TokenBlacklist 엔티티 생성 성공")
    void createTokenBlacklistSuccess() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .accessToken("test-access-token")
                .expiresAt(expiresAt)
                .build();

        assertThat(blacklist.getAccessToken()).isEqualTo("test-access-token");
        assertThat(blacklist.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("토큰 만료 확인 - 만료되지 않음")
    void isNotExpired() {
        LocalDateTime futureExpiresAt = LocalDateTime.now().plusHours(1);

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .accessToken("test-token")
                .expiresAt(futureExpiresAt)
                .build();

        assertThat(blacklist.isExpired()).isFalse();
    }

    @Test
    @DisplayName("토큰 만료 확인 - 만료됨")
    void isExpired() {
        LocalDateTime pastExpiresAt = LocalDateTime.now().minusHours(1);

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .accessToken("test-token")
                .expiresAt(pastExpiresAt)
                .build();

        assertThat(blacklist.isExpired()).isTrue();
    }
}
