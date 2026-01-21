package com.lol.highlight.global.config;

import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * FastAPI AI 서버 연동 설정
 *
 * TODO: [임시 URL 설정]
 * 현재 기본값은 http://localhost:8000 입니다.
 * 실제 FastAPI 서버 배포 후 환경변수 AI_SERVER_URL로 설정하세요.
 *
 * 예시:
 * - 로컬 개발: http://localhost:8000
 * - 개발 서버: http://dev-ai-server.example.com
 * - 운영 서버: http://ai-server.example.com
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ai.server")
@Getter
@Setter
public class AiClientConfig {

    /**
     * FastAPI 서버 기본 URL
     * TODO: FastAPI 서버 배포 후 실제 URL로 변경
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * 요청 타임아웃 (밀리초)
     */
    private long timeout = 30000;

    /**
     * 연결 타임아웃 (밀리초)
     */
    private long connectTimeout = 5000;

    /**
     * 재시도 횟수
     */
    private int maxRetries = 3;

    @Bean
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder) {
        log.info("Initializing AI RestTemplate with baseUrl: {}, timeout: {}ms", baseUrl, timeout);

        return builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .errorHandler(new AiServerErrorHandler())
                .build();
    }

    /**
     * AI 서버 응답 에러 핸들러
     */
    private static class AiServerErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            int statusCode = response.getStatusCode().value();
            String statusText = response.getStatusText();

            log.error("AI Server error - Status: {}, Message: {}", statusCode, statusText);

            if (statusCode >= 500) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR,
                        "AI 서버 내부 오류: " + statusText);
            } else if (statusCode == 408 || statusCode == 504) {
                throw new BusinessException(ErrorCode.AI_SERVER_TIMEOUT,
                        "AI 서버 응답 시간 초과");
            } else if (statusCode >= 400) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR,
                        "AI 서버 요청 오류: " + statusText);
            }
        }
    }
}
