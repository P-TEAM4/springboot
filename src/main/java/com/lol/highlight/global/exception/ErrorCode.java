package com.lol.highlight.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "Internal server error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "Invalid input value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "Method not allowed"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "Entity not found"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "Invalid type value"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "Access is denied"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "U002", "Email is duplicated"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U003", "Invalid credentials"),

    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "Match not found"),
    RIOT_API_ERROR(HttpStatus.BAD_GATEWAY, "M002", "Riot API error"),
    SUMMONER_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "Summoner not found"),

    // Highlight
    HIGHLIGHT_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "Highlight not found"),
    HIGHLIGHT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "H002", "Highlight generation failed"),
    VIDEO_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "H003", "Video processing error"),

    // Analysis
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "Analysis not found"),
    ANALYSIS_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A002", "Analysis processing error"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "Expired token"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH004", "Refresh token not found");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
