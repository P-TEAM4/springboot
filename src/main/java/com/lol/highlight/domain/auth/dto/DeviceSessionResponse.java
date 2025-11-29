package com.lol.highlight.domain.auth.dto;

import com.lol.highlight.domain.auth.entity.DeviceSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "디바이스 세션 응답")
public class DeviceSessionResponse {

    @Schema(description = "디바이스 세션 ID")
    private Long id;

    @Schema(description = "디바이스 고유 ID")
    private String deviceId;

    @Schema(description = "디바이스 이름")
    private String deviceName;

    @Schema(description = "운영체제 정보")
    private String os;

    @Schema(description = "앱 버전")
    private String appVersion;

    @Schema(description = "IP 주소")
    private String ipAddress;

    @Schema(description = "마지막 접속 시간")
    private LocalDateTime lastAccessedAt;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "현재 디바이스 여부")
    private Boolean current;

    public static DeviceSessionResponse from(DeviceSession deviceSession) {
        return DeviceSessionResponse.builder()
                .id(deviceSession.getId())
                .deviceId(deviceSession.getDeviceId())
                .deviceName(deviceSession.getDeviceName())
                .os(deviceSession.getOs())
                .appVersion(deviceSession.getAppVersion())
                .ipAddress(deviceSession.getIpAddress())
                .lastAccessedAt(deviceSession.getLastAccessedAt())
                .createdAt(deviceSession.getCreatedAt())
                .current(false)
                .build();
    }

    public static DeviceSessionResponse from(DeviceSession deviceSession, String currentDeviceId) {
        return DeviceSessionResponse.builder()
                .id(deviceSession.getId())
                .deviceId(deviceSession.getDeviceId())
                .deviceName(deviceSession.getDeviceName())
                .os(deviceSession.getOs())
                .appVersion(deviceSession.getAppVersion())
                .ipAddress(deviceSession.getIpAddress())
                .lastAccessedAt(deviceSession.getLastAccessedAt())
                .createdAt(deviceSession.getCreatedAt())
                .current(deviceSession.getDeviceId().equals(currentDeviceId))
                .build();
    }
}
