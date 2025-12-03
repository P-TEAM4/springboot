package com.lol.highlight.domain.session.entity;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.enums.UserRole;
import com.lol.highlight.global.auth.enums.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceSessionTest {

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
    @DisplayName("DeviceSession 엔티티 생성 성공")
    void createDeviceSessionSuccess() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("refresh-token-123")
                .expiresAt(expiresAt)
                .deviceName("iPhone 15")
                .deviceType("mobile")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        assertThat(session.getUser()).isEqualTo(user);
        assertThat(session.getDeviceId()).isEqualTo("device-123");
        assertThat(session.getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(session.getDeviceName()).isEqualTo("iPhone 15");
        assertThat(session.getDeviceType()).isEqualTo("mobile");
        assertThat(session.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(session.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(session.getLastAccessedAt()).isNotNull();
    }

    @Test
    @DisplayName("리프레시 토큰 업데이트 성공")
    void updateRefreshTokenSuccess() {
        LocalDateTime oldExpiresAt = LocalDateTime.now().plusDays(7);
        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("old-token")
                .expiresAt(oldExpiresAt)
                .build();

        LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(14);
        session.updateRefreshToken("new-token", newExpiresAt);

        assertThat(session.getRefreshToken()).isEqualTo("new-token");
        assertThat(session.getExpiresAt()).isEqualTo(newExpiresAt);
    }

    @Test
    @DisplayName("마지막 접근 시간 업데이트 성공")
    void updateLastAccessedAtSuccess() {
        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("token-123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        LocalDateTime beforeUpdate = session.getLastAccessedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        session.updateLastAccessedAt();

        assertThat(session.getLastAccessedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("디바이스 정보 업데이트 성공")
    void updateDeviceInfoSuccess() {
        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("token-123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .deviceName("Old Device")
                .deviceType("desktop")
                .ipAddress("192.168.1.1")
                .userAgent("Old Agent")
                .build();

        session.updateDeviceInfo("New Device", "mobile", "192.168.1.2", "New Agent");

        assertThat(session.getDeviceName()).isEqualTo("New Device");
        assertThat(session.getDeviceType()).isEqualTo("mobile");
        assertThat(session.getIpAddress()).isEqualTo("192.168.1.2");
        assertThat(session.getUserAgent()).isEqualTo("New Agent");
    }

    @Test
    @DisplayName("세션 만료 확인 - 만료되지 않음")
    void isNotExpired() {
        LocalDateTime futureExpiresAt = LocalDateTime.now().plusDays(7);
        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("token-123")
                .expiresAt(futureExpiresAt)
                .build();

        assertThat(session.isExpired()).isFalse();
    }

    @Test
    @DisplayName("세션 만료 확인 - 만료됨")
    void isExpired() {
        LocalDateTime pastExpiresAt = LocalDateTime.now().minusDays(1);
        DeviceSession session = DeviceSession.builder()
                .user(user)
                .deviceId("device-123")
                .refreshToken("token-123")
                .expiresAt(pastExpiresAt)
                .build();

        assertThat(session.isExpired()).isTrue();
    }
}
