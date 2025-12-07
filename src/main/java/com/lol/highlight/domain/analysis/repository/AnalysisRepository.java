package com.lol.highlight.domain.analysis.repository;

import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByMatchId(Long matchId);

    @Query("SELECT a FROM Analysis a WHERE a.match.puuid = :puuid")
    Page<Analysis> findByPuuid(@Param("puuid") String puuid, Pageable pageable);

    @Query("SELECT a FROM Analysis a WHERE a.match.puuid = :puuid AND a.status = :status")
    List<Analysis> findByPuuidAndStatus(@Param("puuid") String puuid, @Param("status") AnalysisStatus status);

    boolean existsByMatchId(Long matchId);
}
