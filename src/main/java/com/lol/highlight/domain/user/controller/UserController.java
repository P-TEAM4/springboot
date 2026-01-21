package com.lol.highlight.domain.user.controller;

import com.lol.highlight.domain.user.dto.RiotAccountLinkRequest;
import com.lol.highlight.domain.user.dto.UserResponse;
import com.lol.highlight.domain.user.dto.UserUpdateRequest;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.service.UserService;
import com.lol.highlight.global.auth.annotation.AuthUser;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "현재 로그인한 사용자 정보 조회", description = "JWT 토큰을 통해 현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.USER_NOT_FOUND
    })
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthUser User user) {
        return ApiResponse.success("현재 사용자 정보 조회 성공", UserResponse.from(user));
    }

    @Operation(summary = "사용자 정보 조회", description = "사용자 ID로 특정 사용자의 정보를 조회합니다. 비로그인 사용자도 조회 가능합니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND
    })
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id) {
        UserResponse userResponse = userService.getUserById(id);
        return ApiResponse.success("사용자 정보 조회 성공", userResponse);
    }

    @Operation(summary = "사용자 정보 수정", description = "현재 로그인한 사용자의 프로필 정보를 수정합니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PutMapping
    public ApiResponse<UserResponse> updateUser(
            @AuthUser User user,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse userResponse = userService.updateUser(user.getId(), request);
        return ApiResponse.success("사용자 정보 수정 성공", userResponse);
    }

    @Operation(summary = "Riot 계정 연동", description = "현재 로그인한 사용자의 Riot 계정을 연동합니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.EXTERNAL_API_ERROR,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping("/link-riot")
    public ApiResponse<UserResponse> linkRiotAccount(
            @AuthUser User user,
            @Valid @RequestBody RiotAccountLinkRequest request) {
        UserResponse userResponse = userService.linkRiotAccount(user.getId(), request);
        return ApiResponse.success("Riot 계정 연동 성공", userResponse);
    }

    @Operation(summary = "사용자 삭제", description = "현재 로그인한 사용자의 계정을 삭제합니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @DeleteMapping
    public ApiResponse<Void> deleteUser(@AuthUser User user) {
        userService.deleteUser(user.getId());
        return ApiResponse.success("사용자 삭제 성공");
    }
}
