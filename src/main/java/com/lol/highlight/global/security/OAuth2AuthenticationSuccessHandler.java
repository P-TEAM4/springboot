package com.lol.highlight.global.security;

import com.lol.highlight.domain.auth.dto.DeviceInfoRequest;
import com.lol.highlight.domain.auth.dto.TokenResponse;
import com.lol.highlight.domain.auth.service.DeviceSessionService;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final DeviceSessionService deviceSessionService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed.");
            return;
        }

        String targetUrl = determineTargetUrl(request, response, authentication);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        DeviceInfoRequest deviceInfo = extractDeviceInfo(request);
        String ipAddress = getClientIpAddress(request);

        TokenResponse tokenResponse = deviceSessionService.createOrUpdateSession(user, deviceInfo, ipAddress);

        log.info("OAuth2 login success: user={}, deviceId={}", email, deviceInfo.getDeviceId());

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("refreshToken", tokenResponse.getRefreshToken())
                .build().toUriString();
    }

    private DeviceInfoRequest extractDeviceInfo(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        String deviceName = request.getHeader("X-Device-Name");
        String os = request.getHeader("X-Device-OS");
        String appVersion = request.getHeader("X-App-Version");

        if (!StringUtils.hasText(deviceId)) {
            deviceId = "unknown-device-" + System.currentTimeMillis();
            log.warn("Device ID not provided, using generated ID: {}", deviceId);
        }

        return new DeviceInfoRequest(deviceId, deviceName, os, appVersion);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
