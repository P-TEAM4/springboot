package com.lol.highlight.domain.auth.service;

import com.lol.highlight.domain.auth.entity.TokenBlacklist;
import com.lol.highlight.domain.auth.repository.TokenBlacklistRepository;
import com.lol.highlight.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void addToBlacklist(String token, String reason) {
        if (tokenBlacklistRepository.existsByToken(token)) {
            log.warn("Token already in blacklist: {}", token);
            return;
        }

        LocalDateTime expiryDate = jwtTokenProvider.getExpirationAsLocalDateTime(token);

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .token(token)
                .expiryDate(expiryDate)
                .reason(reason)
                .build();

        tokenBlacklistRepository.save(blacklist);
        log.info("Token added to blacklist. Reason: {}", reason);
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired blacklisted tokens");
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleanup completed");
    }
}
