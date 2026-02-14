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
    // MySQL의 서브쿼리 LIMIT 제한 우회: 서브쿼리를 한 번 더 래핑
    @Modifying
    @Query(value = "DELETE FROM matches WHERE puuid = :puuid AND id NOT IN " +
           "(SELECT id FROM (SELECT id FROM matches WHERE puuid = :puuid ORDER BY game_creation DESC LIMIT :keepCount) AS temp)",
           nativeQuery = true)
    void deleteOldMatchesKeepRecent(@Param("puuid") String puuid, @Param("keepCount") int keepCount);

    // TTL 관리용
    void deleteByCreatedAtBefore(LocalDateTime expiryDate);

    // 모든 고유 puuid 조회
    @Query("SELECT DISTINCT m.puuid FROM Match m")
    java.util.List<String> findDistinctPuuids();
}
