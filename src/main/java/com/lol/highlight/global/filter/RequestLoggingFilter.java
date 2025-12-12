package com.lol.highlight.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 요청 시작 시간 기록
        long startTime = System.currentTimeMillis();

        // 요청 정보 로깅
        logRequest(httpRequest);

        try {
            // 다음 필터로 진행
            chain.doFilter(request, response);
        } finally {
            // 응답 정보 로깅
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, duration);
        }
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;

        log.info("=================================================================");
        log.info("[REQUEST] {} {}", method, fullUrl);
        log.info("Remote IP: {}", getClientIp(request));
        log.info("User-Agent: {}", request.getHeader("User-Agent"));
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        String statusText = getStatusText(status);

        log.info("[RESPONSE] {} {} - Status: {} ({}) - Duration: {}ms",
                method, uri, status, statusText, duration);
        log.info("=================================================================");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getStatusText(int status) {
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        } else if (status >= 300 && status < 400) {
            return "REDIRECT";
        } else if (status >= 400 && status < 500) {
            return "CLIENT_ERROR";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "UNKNOWN";
    }
}
