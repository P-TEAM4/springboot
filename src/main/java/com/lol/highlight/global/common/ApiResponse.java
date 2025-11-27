package com.lol.highlight.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lol.highlight.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답")
public class    ApiResponse<T> {

    @Schema(description = "응답 시간", example = "2025-11-27T12:00:00.000000")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(description = "응답 코드", example = "SUCCESS")
    private String code;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private String message;

    @Schema(description = "요청 경로", example = "/api/users/1")
    private String path;

    @Schema(description = "응답 데이터")
    private T data;

    // 성공 응답 (데이터 있음)
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .code("SUCCESS")
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .build();
    }

    // 성공 응답 (커스텀 메시지, 데이터)
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .code("SUCCESS")
                .message("요청이 성공적으로 처리되었습니다.")
                .build();
    }

    // 성공 응답 (커스텀 메시지만)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .code("SUCCESS")
                .message(message)
                .build();
    }

    // 생성 성공 응답
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(201)
                .code("CREATED")
                .message("리소스가 성공적으로 생성되었습니다.")
                .data(data)
                .build();
    }

    // 생성 성공 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(201)
                .code("CREATED")
                .message(message)
                .data(data)
                .build();
    }

    // 수락 응답 (비동기 작업)
    public static <T> ApiResponse<T> accepted() {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(202)
                .code("ACCEPTED")
                .message("요청이 접수되었습니다.")
                .build();
    }

    // 수락 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> accepted(String message) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(202)
                .code("ACCEPTED")
                .message(message)
                .build();
    }

    // 에러 응답 (status, code, message)
    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

    // 에러 응답 (status, code, message, path)
    public static <T> ApiResponse<T> error(int status, String code, String message, String path) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    // 에러 응답 (ErrorCode)
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // 에러 응답 (ErrorCode, path)
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String path) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .build();
    }
}
