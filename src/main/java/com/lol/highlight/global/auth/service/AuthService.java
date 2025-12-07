package com.lol.highlight.global.auth.service;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.enums.UserRole;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.auth.enums.AuthProvider;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Transactional
    public void loginWithGoogle(String idToken, HttpServletResponse response) {
        // 1. Google ID Token 검증
        Map<String, Object> googleUser = verifyGoogleIdToken(idToken);

        String email = (String) googleUser.get("email");
        String name = (String) googleUser.get("name");
        String providerId = (String) googleUser.get("sub");

        // 2. 사용자 조회 또는 생성
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, providerId));

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createTokenWithUserId(user.getId());
        String refreshToken = jwtTokenProvider.createTokenWithUserId(user.getId());

        // 4. 응답 헤더에 토큰 추가
        response.setHeader("Access-Token", accessToken);
        response.setHeader("Refresh-Token", refreshToken);

        log.info("User logged in: userId={}, email={}", user.getId(), email);
    }

    @Transactional
    public void refreshToken(String refreshToken, HttpServletResponse response) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 블랙리스트 확인
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BusinessException(ErrorCode.BLACKLISTED_TOKEN);
        }

        // 3. 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 4. 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.createTokenWithUserId(userId);
        String newRefreshToken = jwtTokenProvider.createTokenWithUserId(userId);

        // 5. 기존 Refresh Token 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(refreshToken);

        // 6. 응답 헤더에 새 토큰 추가
        response.setHeader("Access-Token", newAccessToken);
        response.setHeader("Refresh-Token", newRefreshToken);

        log.info("Token refreshed: userId={}", userId);
    }

    @Transactional
    public void logout(String accessToken) {
        // Access Token을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(accessToken);
        log.info("User logged out");
    }

    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            String url = GOOGLE_TOKEN_INFO_URL + idToken;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.containsKey("error")) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Google 토큰 검증 실패");
        }
    }

    private User createNewUser(String email, String name, String providerId) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .role(UserRole.USER)
                .build();

        return userRepository.save(newUser);
    }
}
