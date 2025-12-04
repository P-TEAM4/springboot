package com.lol.highlight.global.auth.service;

import com.lol.highlight.global.auth.entity.TokenBlacklist;
import com.lol.highlight.global.auth.jwt.JwtTokenProvider;
import com.lol.highlight.global.auth.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void addToBlacklist(String accessToken) {
        if (tokenBlacklistRepository.existsByAccessToken(accessToken)) {
            log.debug("Token already in blacklist");
            return;
        }

        Date expiration = jwtTokenProvider.getExpirationFromToken(accessToken);
        LocalDateTime expiresAt = expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .accessToken(accessToken)
                .expiresAt(expiresAt)
                .build();

        tokenBlacklistRepository.save(blacklist);
        log.info("Token added to blacklist");
    }

    public boolean isBlacklisted(String accessToken) {
        return tokenBlacklistRepository.existsByAccessToken(accessToken);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.debug("Expired tokens cleaned up from blacklist");
    }
}
