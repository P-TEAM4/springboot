package com.lol.highlight.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testSecret = "test-secret-key-for-jwt-token-provider-test-minimum-32-characters";
    private long accessTokenValidityInMilliseconds = 3600000; // 1 hour
    private long refreshTokenValidityInMilliseconds = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(testSecret, accessTokenValidityInMilliseconds, refreshTokenValidityInMilliseconds);
    }

    @Test
    @DisplayName("Authentication으로 Access Token 생성 성공")
    void createAccessTokenWithAuthenticationSuccess() {
        // given
        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn("test@example.com");

        // when
        String token = jwtTokenProvider.createAccessToken(authentication);

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("이메일로 Access Token 생성 성공")
    void createAccessTokenWithEmailSuccess() {
        // given
        String email = "test@example.com";

        // when
        String token = jwtTokenProvider.createAccessToken(email);

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자 이름 추출 성공")
    void getUsernameFromTokenSuccess() {
        // given
        String email = "test@example.com";
        String token = jwtTokenProvider.createAccessToken(email);

        // when
        String extractedEmail = jwtTokenProvider.getUsernameFromToken(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 성공")
    void validateTokenSuccess() {
        // given
        String email = "test@example.com";
        String token = jwtTokenProvider.createAccessToken(email);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 검증 실패")
    void validateTokenFail() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 실패")
    void validateExpiredTokenFail() {
        // given - create expired token
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(testSecret, -1000, refreshTokenValidityInMilliseconds);
        String expiredToken = expiredTokenProvider.createAccessToken("test@example.com");

        // when
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("JWT 토큰의 Claims 확인")
    void verifyTokenClaims() {
        // given
        String email = "test@example.com";
        String token = jwtTokenProvider.createAccessToken(email);

        // when
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // then
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime())
                .isGreaterThan(claims.getIssuedAt().getTime());
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 JWT 토큰 검증 실패")
    void validateTokenWithDifferentSecretFail() {
        // given
        String differentSecret = "different-secret-key-for-jwt-token-test-minimum-32-characters-long";
        JwtTokenProvider differentProvider = new JwtTokenProvider(differentSecret, accessTokenValidityInMilliseconds, refreshTokenValidityInMilliseconds);
        String token = differentProvider.createAccessToken("test@example.com");

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }
}
