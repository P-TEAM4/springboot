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

    Page<Highlight> findByMatchId(Long matchId, Pageable pageable);

    List<Highlight> findByMatchIdAndStatus(Long matchId, HighlightStatus status);

    @Query("SELECT h FROM Highlight h WHERE h.match.user.id = :userId")
    Page<Highlight> findByUserId(@Param("userId") Long userId, Pageable pageable);

    List<Highlight> findByMatchIdAndType(Long matchId, HighlightType type);

    @Query("SELECT h FROM Highlight h WHERE h.match.user.id = :userId AND h.status = :status")
    List<Highlight> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") HighlightStatus status);
}
