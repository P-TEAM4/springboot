package com.lol.highlight.global.auth.repository;

import com.lol.highlight.global.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByAccessToken(String accessToken);

    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
