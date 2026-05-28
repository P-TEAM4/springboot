package com.lol.highlight.domain.highlight.repository;

import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.enums.HighlightType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HighlightRepository extends JpaRepository<Highlight, Long> {

    Page<Highlight> findByMatch_MatchId(String matchId, Pageable pageable);

    List<Highlight> findByMatch_MatchId(String matchId);

    void deleteAllByMatch_MatchId(String matchId);

    List<Highlight> findByMatch_MatchIdAndStatus(String matchId, HighlightStatus status);

    @Query("SELECT h FROM Highlight h WHERE h.match.puuid = :puuid")
    Page<Highlight> findByPuuid(@Param("puuid") String puuid, Pageable pageable);

    List<Highlight> findByMatch_MatchIdAndType(String matchId, HighlightType type);

    @Query("SELECT h FROM Highlight h WHERE h.match.puuid = :puuid AND h.status = :status")
    List<Highlight> findByPuuidAndStatus(@Param("puuid") String puuid, @Param("status") HighlightStatus status);
}
