package com.lol.highlight.domain.auth.entity;

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
        @Index(name = "idx_device_id", columnList = "deviceId"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_user_revoked", columnList = "user_id, revoked")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String deviceId;

    @Column(length = 100)
    private String deviceName;

    @Column(length = 50)
    private String os;

    @Column(length = 50)
    private String appVersion;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 500)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime refreshTokenExpiryDate;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(nullable = false)
    private LocalDateTime lastAccessedAt;

    @Builder
    public DeviceSession(User user, String deviceId, String deviceName,
                         String os, String appVersion, String ipAddress,
                         String refreshToken, LocalDateTime refreshTokenExpiryDate) {
        this.user = user;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.os = os;
        this.appVersion = appVersion;
        this.ipAddress = ipAddress;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiryDate = refreshTokenExpiryDate;
        this.revoked = false;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiryDate = expiryDate;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(refreshTokenExpiryDate);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
