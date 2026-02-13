package com.lol.highlight.domain.user.controller;

import com.lol.highlight.domain.user.dto.UserSettingsResponse;
import com.lol.highlight.domain.user.dto.UserSettingsUpdateRequest;
import com.lol.highlight.domain.user.service.UserSettingsService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Settings", description = "사용자 설정 API")
@RestController
@RequestMapping("/api/users/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @Operation(summary = "사용자 설정 조회", description = "현재 로그인한 사용자의 설정을 조회합니다.")
    @ApiErrorExamples({ErrorCode.AUTHENTICATION_REQUIRED})
    @GetMapping
    public ApiResponse<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        UserSettingsResponse settings = userSettingsService.getSettings(userId);
        
        return ApiResponse.success(settings);
    }

    @Operation(summary = "사용자 설정 업데이트", description = "현재 로그인한 사용자의 설정을 업데이트합니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PutMapping
    public ApiResponse<UserSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserSettingsUpdateRequest request) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        UserSettingsResponse settings = userSettingsService.updateSettings(userId, request);
        
        return ApiResponse.success(settings);
    }
}
