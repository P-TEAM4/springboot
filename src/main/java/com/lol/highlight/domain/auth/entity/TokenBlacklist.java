package com.lol.highlight.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expiry_date", columnList = "expiryDate")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private LocalDateTime blacklistedAt;

    private String reason;

    @Builder
    public TokenBlacklist(String token, LocalDateTime expiryDate, String reason) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.blacklistedAt = LocalDateTime.now();
        this.reason = reason;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
