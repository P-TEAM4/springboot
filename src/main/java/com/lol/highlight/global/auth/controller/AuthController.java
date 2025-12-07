package com.lol.highlight.global.auth.controller;

import com.lol.highlight.global.auth.dto.GoogleLoginRequest;
import com.lol.highlight.global.auth.service.AuthService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}
