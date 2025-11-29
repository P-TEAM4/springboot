package com.lol.highlight.domain.auth.service;

import com.lol.highlight.domain.auth.dto.DeviceInfoRequest;
import com.lol.highlight.domain.auth.dto.DeviceSessionResponse;
import com.lol.highlight.domain.auth.dto.TokenResponse;
import com.lol.highlight.domain.auth.entity.DeviceSession;
import com.lol.highlight.domain.auth.repository.DeviceSessionRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceSessionService {

    private final DeviceSessionRepository deviceSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse createOrUpdateSession(User user, DeviceInfoRequest deviceInfo, String ipAddress) {
        String email = user.getEmail();

        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        LocalDateTime refreshTokenExpiryDate = jwtTokenProvider.getRefreshTokenExpiryDate();

        DeviceSession deviceSession = deviceSessionRepository
                .findByUserAndDeviceId(user, deviceInfo.getDeviceId())
                .orElse(null);

        if (deviceSession != null) {
            if (deviceSession.getRevoked()) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "This device has been revoked");
            }
            deviceSession.updateRefreshToken(refreshToken, refreshTokenExpiryDate);
            log.info("Updated existing device session: deviceId={}, user={}", deviceInfo.getDeviceId(), email);
        } else {
            deviceSession = DeviceSession.builder()
                    .user(user)
                    .deviceId(deviceInfo.getDeviceId())
                    .deviceName(deviceInfo.getDeviceName())
                    .os(deviceInfo.getOs())
                    .appVersion(deviceInfo.getAppVersion())
                    .ipAddress(ipAddress)
                    .refreshToken(refreshToken)
                    .refreshTokenExpiryDate(refreshTokenExpiryDate)
                    .build();
            deviceSessionRepository.save(deviceSession);
            log.info("Created new device session: deviceId={}, user={}", deviceInfo.getDeviceId(), email);
        }

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public TokenResponse rotateRefreshToken(String refreshToken, String deviceId) {
        DeviceSession deviceSession = deviceSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token"));

        if (!deviceSession.getDeviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Device ID mismatch");
        }

        if (deviceSession.getRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "This device session has been revoked");
        }

        if (deviceSession.isExpired()) {
            deviceSessionRepository.delete(deviceSession);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired");
        }

        String email = deviceSession.getUser().getEmail();
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);
        LocalDateTime newExpiryDate = jwtTokenProvider.getRefreshTokenExpiryDate();

        deviceSession.updateRefreshToken(newRefreshToken, newExpiryDate);

        log.info("Rotated refresh token for device: deviceId={}, user={}", deviceId, email);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public List<DeviceSessionResponse> getActiveDevices(User user, String currentDeviceId) {
        List<DeviceSession> sessions = deviceSessionRepository.findByUserAndRevokedFalse(user);

        return sessions.stream()
                .map(session -> DeviceSessionResponse.from(session, currentDeviceId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeDevice(User user, String deviceId) {
        DeviceSession deviceSession = deviceSessionRepository.findByUserAndDeviceId(user, deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Device session not found"));

        deviceSession.revoke();
        log.info("Revoked device session: deviceId={}, user={}", deviceId, user.getEmail());
    }

    @Transactional
    public void revokeOtherDevices(User user, String currentDeviceId) {
        deviceSessionRepository.revokeOtherDevices(user, currentDeviceId);
        log.info("Revoked all other devices for user: {}, kept deviceId: {}", user.getEmail(), currentDeviceId);
    }

    @Transactional
    public void revokeAllDevices(User user) {
        deviceSessionRepository.revokeAllDevices(user);
        log.info("Revoked all devices for user: {}", user.getEmail());
    }

    @Transactional
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        deviceSessionRepository.deleteExpiredSessions(now);
        log.info("Cleaned up expired device sessions at {}", now);
    }
}
