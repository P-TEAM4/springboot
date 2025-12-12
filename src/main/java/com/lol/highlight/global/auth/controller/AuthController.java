package com.lol.highlight.global.auth.controller;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.auth.dto.GoogleLoginRequest;
import com.lol.highlight.global.auth.dto.TokenResponse;
import com.lol.highlight.global.auth.jwt.JwtTokenProvider;
import com.lol.highlight.global.auth.service.AuthService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Google OAuth 로그인", description = "Electron 앱에서 받은 Google ID Token으로 로그인합니다. Access Token과 Refresh Token은 응답 헤더로 전달됩니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_TOKEN,
            ErrorCode.EXTERNAL_API_ERROR
    })
    @PostMapping("/google")
    public ApiResponse<Void> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        authService.loginWithGoogle(request.getIdToken(), response);
        return ApiResponse.success("로그인 성공");
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급받습니다. 새로운 토큰들은 응답 헤더로 전달됩니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_TOKEN,
            ErrorCode.TOKEN_EXPIRED
    })
    @PostMapping("/refresh")
    public ApiResponse<Void> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken,
            HttpServletResponse response) {
        authService.refreshToken(refreshToken, response);
        return ApiResponse.success("토큰 갱신 성공");
    }

    @Operation(summary = "로그아웃", description = "현재 토큰을 블랙리스트에 추가합니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_TOKEN
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return ApiResponse.success("로그아웃 되었습니다");
    }

    @Operation(
            summary = "[DEV] 테스트용 JWT 토큰 발급",
            description = "개발 환경에서 Swagger 테스트를 위한 JWT 토큰을 즉시 발급합니다. " +
                    "이메일을 파라미터로 받아 해당 유저가 없으면 자동으로 생성합니다. " +
                    "프로덕션 환경에서는 비활성화됩니다."
    )
    @PostMapping("/test-token")
    public ApiResponse<TokenResponse> generateTestToken(
            @Parameter(description = "테스트 유저 이메일", example = "test@example.com")
            @RequestParam(defaultValue = "test@example.com") String email) {

        User user = authService.createOrGetTestUser(email);

        String accessToken = jwtTokenProvider.createTokenWithUserId(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshTokenWithUserId(user.getId());

        TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail()
        );

        return ApiResponse.success("테스트 토큰 발급 성공", tokenResponse);
    }
}
