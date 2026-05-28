package com.lol.highlight.domain.match.repository;

import com.lol.highlight.domain.match.entity.MatchBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchBanRepository extends JpaRepository<MatchBan, Long> {

    boolean existsByMatchId(String matchId);

    @Query("SELECT b.championName AS championName, COUNT(b) AS banCount " +
           "FROM MatchBan b GROUP BY b.championName")
    List<BanStatsProjection> findBanStats();

    interface BanStatsProjection {
        String getChampionName();
        Long getBanCount();
    }
}
