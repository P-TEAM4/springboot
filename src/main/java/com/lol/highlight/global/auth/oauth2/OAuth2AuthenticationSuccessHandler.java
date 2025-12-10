package com.lol.highlight.global.auth.oauth2;

import com.lol.highlight.global.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed.");
            return;
        }

        try {
            String targetUrl = determineTargetUrl(request, response, authentication);
            clearAuthenticationAttributes(request);
            log.info("OAuth2 authentication successful, redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("Error during OAuth2 success handling", e);
            String errorUrl = redirectUri.replace("/auth/callback", "/auth/error")
                    + "?error=" + URLEncoder.encode("Authentication failed", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // userId 추출 (CustomOAuth2UserService에서 추가한 값)
        Long userId = getLongAttribute(oAuth2User, "userId");
        if (userId == null) {
            throw new IllegalStateException("User ID not found in OAuth2User attributes");
        }

        // Access token과 Refresh token 생성
        String accessToken = tokenProvider.createTokenWithUserId(userId);
        String refreshToken = tokenProvider.createRefreshTokenWithUserId(userId);

        log.info("Generated tokens for userId: {}", userId);

        // 딥링크로 리다이렉트
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
    }

    private Long getLongAttribute(OAuth2User oAuth2User, String attributeName) {
        Object value = oAuth2User.getAttribute(attributeName);
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }
}
