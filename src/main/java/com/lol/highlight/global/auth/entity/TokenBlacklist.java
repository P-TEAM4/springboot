package com.lol.highlight.global.auth.entity;

import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_access_token", columnList = "accessToken"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenBlacklist extends BaseEntity {

    @Column(nullable = false, unique = true, length = 512)
    private String accessToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public TokenBlacklist(String accessToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
