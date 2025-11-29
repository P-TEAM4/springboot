package com.lol.highlight.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "디바이스 정보 요청")
public class DeviceInfoRequest {

    @NotBlank(message = "Device ID is required")
    @Size(max = 100)
    @Schema(description = "디바이스 고유 ID", example = "abc123-uuid-456")
    private String deviceId;

    @Size(max = 100)
    @Schema(description = "디바이스 이름", example = "MacBook Pro")
    private String deviceName;

    @Size(max = 50)
    @Schema(description = "운영체제 정보", example = "macOS 14.0")
    private String os;

    @Size(max = 50)
    @Schema(description = "앱 버전", example = "1.0.0")
    private String appVersion;
}
