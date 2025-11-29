package com.lol.highlight.domain.auth.controller;

import com.lol.highlight.domain.auth.dto.DeviceSessionResponse;
import com.lol.highlight.domain.auth.dto.TokenRefreshRequest;
import com.lol.highlight.domain.auth.dto.TokenResponse;
import com.lol.highlight.domain.auth.service.DeviceSessionService;
import com.lol.highlight.domain.auth.service.TokenBlacklistService;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.config.SwaggerConfig.ApiErrorExamples;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceSessionService deviceSessionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token과 Device ID를 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다 (RTR).")
    @ApiErrorExamples({ErrorCode.INVALID_TOKEN, ErrorCode.TOKEN_EXPIRED})
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenResponse response = deviceSessionService.rotateRefreshToken(
                request.getRefreshToken(),
                request.getDeviceId()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 (현재 디바이스)", description = "현재 디바이스의 세션을 무효화하고 Access Token을 블랙리스트에 추가합니다.")
    @ApiErrorExamples({ErrorCode.INVALID_TOKEN, ErrorCode.USER_NOT_FOUND})
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @RequestParam String deviceId,
            Authentication authentication
    ) {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getUsernameFromToken(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            tokenBlacklistService.addToBlacklist(token, "USER_LOGOUT");
            deviceSessionService.revokeDevice(user, deviceId);

            log.info("User logged out from device: user={}, deviceId={}", email, deviceId);
        }

        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

    @GetMapping("/devices")
    @Operation(summary = "내 디바이스 목록 조회", description = "현재 사용자의 활성화된 디바이스 세션 목록을 조회합니다.")
    @ApiErrorExamples({ErrorCode.USER_NOT_FOUND})
    public ResponseEntity<ApiResponse<List<DeviceSessionResponse>>> getMyDevices(
            Authentication authentication,
            @RequestParam String currentDeviceId
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<DeviceSessionResponse> devices = deviceSessionService.getActiveDevices(user, currentDeviceId);

        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "특정 디바이스 로그아웃", description = "특정 디바이스의 세션을 무효화합니다.")
    @ApiErrorExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND})
    public ResponseEntity<ApiResponse<Void>> revokeDevice(
            Authentication authentication,
            @PathVariable String deviceId
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        deviceSessionService.revokeDevice(user, deviceId);

        return ResponseEntity.ok(ApiResponse.success("디바이스 세션이 무효화되었습니다."));
    }

    @DeleteMapping("/devices/others")
    @Operation(summary = "다른 모든 디바이스 로그아웃", description = "현재 디바이스를 제외한 모든 디바이스의 세션을 무효화합니다.")
    @ApiErrorExamples({ErrorCode.USER_NOT_FOUND})
    public ResponseEntity<ApiResponse<Void>> revokeOtherDevices(
            Authentication authentication,
            @RequestParam String currentDeviceId
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        deviceSessionService.revokeOtherDevices(user, currentDeviceId);

        return ResponseEntity.ok(ApiResponse.success("다른 모든 디바이스의 세션이 무효화되었습니다."));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
