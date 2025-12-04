package com.lol.highlight.domain.match.repository;

import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByMatchId(String matchId);

    Page<Match> findByUserId(Long userId, Pageable pageable);

    List<Match> findByUserIdAndStatus(Long userId, MatchStatus status);

    boolean existsByMatchId(String matchId);

    List<Match> findTop20ByUserIdOrderByGameCreationDesc(Long userId);
}
