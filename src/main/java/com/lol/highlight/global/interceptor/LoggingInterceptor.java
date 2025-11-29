package com.lol.highlight.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Enumeration;
import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_ID = "requestId";
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        request.setAttribute(REQUEST_ID, requestId);
        request.setAttribute(START_TIME, startTime);

        log.info("[{}] {} {} - START", requestId, request.getMethod(), request.getRequestURI());

        // Request Headers
        if (log.isDebugEnabled()) {
            log.debug("[{}] Headers:", requestId);
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.debug("[{}]   {}: {}", requestId, headerName, request.getHeader(headerName));
            }
        }

        // Request Parameters
        if (log.isDebugEnabled() && !request.getParameterMap().isEmpty()) {
            log.debug("[{}] Parameters:", requestId);
            request.getParameterMap().forEach((key, values) ->
                log.debug("[{}]   {}: {}", requestId, key, String.join(", ", values))
            );
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        String requestId = (String) request.getAttribute(REQUEST_ID);
        log.debug("[{}] Response Status: {}", requestId, response.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestId = (String) request.getAttribute(REQUEST_ID);
        Long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        if (ex != null) {
            log.error("[{}] {} {} - FAILED ({} ms) - Exception: {}",
                requestId, request.getMethod(), request.getRequestURI(), duration, ex.getMessage());
        } else {
            log.info("[{}] {} {} - COMPLETED ({} ms) - Status: {}",
                requestId, request.getMethod(), request.getRequestURI(), duration, response.getStatus());
        }
    }
}
