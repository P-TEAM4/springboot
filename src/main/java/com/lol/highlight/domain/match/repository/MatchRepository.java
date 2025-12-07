package com.lol.highlight.domain.match.repository;

import com.lol.highlight.domain.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByMatchId(String matchId);

    boolean existsByMatchId(String matchId);

    // puuid 기반 조회
    Page<Match> findByPuuidOrderByGameCreationDesc(String puuid, Pageable pageable);

    long countByPuuid(String puuid);

    // 최근 N개만 유지, 나머지 삭제
    @Modifying
    @Query("DELETE FROM Match m WHERE m.puuid = :puuid AND m.id NOT IN " +
           "(SELECT m2.id FROM Match m2 WHERE m2.puuid = :puuid ORDER BY m2.gameCreation DESC LIMIT :keepCount)")
    void deleteOldMatchesKeepRecent(@Param("puuid") String puuid, @Param("keepCount") int keepCount);

    // TTL 관리용
    void deleteByCreatedAtBefore(LocalDateTime expiryDate);
}
