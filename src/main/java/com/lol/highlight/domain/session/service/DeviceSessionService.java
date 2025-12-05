package com.lol.highlight.domain.session.service;

import com.lol.highlight.domain.session.dto.DeviceSessionResponse;
import com.lol.highlight.domain.session.dto.TokenResponse;
import com.lol.highlight.domain.session.entity.DeviceSession;
import com.lol.highlight.domain.session.repository.DeviceSessionRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.auth.jwt.JwtTokenProvider;
import com.lol.highlight.global.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceSessionService {

    private final DeviceSessionRepository deviceSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenValidityInMilliseconds;

    @Transactional
    public TokenResponse createTokens(User user, String deviceId, String deviceName,
                                     String deviceType, String ipAddress, String userAgent) {
        String accessToken = jwtTokenProvider.createToken(user.getEmail());
        String refreshToken = generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenValidityInMilliseconds / 1000);

        DeviceSession existingSession = deviceSessionRepository
                .findByUserAndDeviceId(user, deviceId)
                .orElse(null);

        if (existingSession != null) {
            existingSession.updateRefreshToken(refreshToken, expiresAt);
            existingSession.updateDeviceInfo(deviceName, deviceType, ipAddress, userAgent);
        } else {
            DeviceSession newSession = DeviceSession.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            deviceSessionRepository.save(newSession);
        }

        return TokenResponse.of(accessToken, refreshToken, refreshTokenValidityInMilliseconds);
    }

    @Transactional
    public TokenResponse refreshAccessToken(String refreshToken, String deviceId) {
        DeviceSession session = deviceSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!session.getDeviceId().equals(deviceId)) {
            throw new IllegalArgumentException("Device ID mismatch");
        }

        if (session.isExpired()) {
            deviceSessionRepository.delete(session);
            throw new IllegalArgumentException("Refresh token expired");
        }

        session.updateLastAccessedAt();

        String newAccessToken = jwtTokenProvider.createToken(session.getUser().getEmail());

        return TokenResponse.of(newAccessToken, refreshToken, refreshTokenValidityInMilliseconds);
    }

    @Transactional
    public void revokeSession(String refreshToken) {
        deviceSessionRepository.findByRefreshToken(refreshToken)
                .ifPresent(deviceSessionRepository::delete);
    }

    @Transactional
    public void revokeUserSession(User user, String deviceId) {
        deviceSessionRepository.deleteByUserAndDeviceId(user, deviceId);
    }

    @Transactional
    public void revokeAllUserSessions(User user) {
        deviceSessionRepository.deleteAllByUser(user);
    }

    public List<DeviceSessionResponse> getUserSessions(User user) {
        return deviceSessionRepository.findAllByUserOrderByLastAccessedAtDesc(user)
                .stream()
                .map(DeviceSessionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupExpiredSessions() {
        deviceSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }

    public long getUserSessionCount(User user) {
        return deviceSessionRepository.countByUser(user);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        tokenBlacklistService.addToBlacklist(accessToken);
        revokeSession(refreshToken);
        log.info("User logged out successfully");
    }

    @Transactional
    public void logoutFromDevice(User user, String accessToken, String deviceId) {
        tokenBlacklistService.addToBlacklist(accessToken);
        revokeUserSession(user, deviceId);
        log.info("User logged out from device: {}", deviceId);
    }

    @Transactional
    public void logoutFromAllDevices(User user, String accessToken) {
        tokenBlacklistService.addToBlacklist(accessToken);
        revokeAllUserSessions(user);
        log.info("User logged out from all devices");
    }

    @Transactional
    public void deleteUserAccount(User user, String accessToken) {
        tokenBlacklistService.addToBlacklist(accessToken);
        revokeAllUserSessions(user);
        log.info("User account deleted, all sessions revoked");
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
