package com.lol.highlight.global.exception;

import com.lol.highlight.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ApiResponse<?> handleBusinessException(final BusinessException e, HttpServletRequest request) {
        log.error("BusinessException: {}", e.getMessage(), e);
        final ErrorCode errorCode = e.getErrorCode();
        return ApiResponse.error(errorCode, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ApiResponse<?> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        log.error("MethodArgumentNotValidException", e);
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                errorMessage != null ? errorMessage : ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ApiResponse<?> handleBindException(BindException e, HttpServletRequest request) {
        log.error("BindException", e);
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                errorMessage != null ? errorMessage : ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ApiResponse<?> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        log.error("MethodArgumentTypeMismatchException", e);
        return ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE, request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    protected ApiResponse<?> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        log.error("HttpRequestMethodNotSupportedException", e);
        return ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected ApiResponse<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("AccessDeniedException", e);
        return ApiResponse.error(ErrorCode.ACCESS_DENIED, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ApiResponse<?> handleException(Exception e, HttpServletRequest request) {
        log.error("Exception: {}", e.getMessage(), e);
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }
}
