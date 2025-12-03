package com.lol.highlight.domain.session.dto;

import com.lol.highlight.domain.session.entity.DeviceSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceSessionResponse {

    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private boolean expired;

    public static DeviceSessionResponse from(DeviceSession deviceSession) {
        return DeviceSessionResponse.builder()
                .id(deviceSession.getId())
                .deviceId(deviceSession.getDeviceId())
                .deviceName(deviceSession.getDeviceName())
                .deviceType(deviceSession.getDeviceType())
                .ipAddress(deviceSession.getIpAddress())
                .lastAccessedAt(deviceSession.getLastAccessedAt())
                .expiresAt(deviceSession.getExpiresAt())
                .expired(deviceSession.isExpired())
                .build();
    }
}
