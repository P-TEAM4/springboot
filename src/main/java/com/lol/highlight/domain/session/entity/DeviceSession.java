package com.lol.highlight.domain.session.entity;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_sessions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_device_id", columnList = "deviceId"),
    @Index(name = "idx_refresh_token", columnList = "refreshToken")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String deviceId;

    @Column(nullable = false, unique = true, length = 512)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String deviceName;

    private String deviceType;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime lastAccessedAt;

    @Builder
    public DeviceSession(User user, String deviceId, String refreshToken,
                        LocalDateTime expiresAt, String deviceName, String deviceType,
                        String ipAddress, String userAgent) {
        this.user = user;
        this.deviceId = deviceId;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateDeviceInfo(String deviceName, String deviceType, String ipAddress, String userAgent) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
