package com.lol.highlight.domain.auth.service;

import com.lol.highlight.domain.auth.entity.RefreshToken;
import com.lol.highlight.domain.auth.repository.RefreshTokenRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
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
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // 기존 리프레시 토큰 모두 무효화 (RTR)
        revokeAllUserTokens(user);

        String token = jwtTokenProvider.createRefreshToken(user.getEmail());
        LocalDateTime expiryDate = jwtTokenProvider.getRefreshTokenExpiryDate();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Refresh token is expired or revoked");
        }

        // 기존 토큰 무효화
        refreshToken.markAsUsed();

        // 새 리프레시 토큰 생성
        return createRefreshToken(refreshToken.getUser());
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Refresh token is expired or revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public void deleteUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 실행
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleanup completed");
    }
}
