package com.lol.highlight.global.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "필수 필드가 누락되었습니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다"),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "블랙리스트에 등록된 토큰입니다"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매치를 찾을 수 없습니다"),
    HIGHLIGHT_NOT_FOUND(HttpStatus.NOT_FOUND, "하이라이트를 찾을 수 없습니다"),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    DUPLICATE_DEVICE(HttpStatus.CONFLICT, "이미 등록된 디바이스입니다"),

    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수 제한을 초과했습니다"),

    IMAGE_SIZE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 크기가 너무 큽니다"),
    IMAGE_FORMAT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다"),
    VIDEO_SIZE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "비디오 크기가 너무 큽니다"),
    VIDEO_FORMAT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 비디오 형식입니다"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다");

    private final HttpStatus status;
    private final String message;
}
