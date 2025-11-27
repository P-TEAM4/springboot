package com.lol.highlight.domain.match.controller;

import com.lol.highlight.domain.match.dto.MatchImportRequest;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long id) {
        MatchResponse match = matchService.getMatchById(id);
        return ResponseEntity.ok(match);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MatchResponse>> getUserMatches(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<MatchResponse> matches = matchService.getUserMatches(userId, pageable);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<MatchResponse>> getRecentMatches(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int count) {
        List<MatchResponse> matches = matchService.getRecentMatches(userId, count);
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/user/{userId}/import")
    public ResponseEntity<MatchResponse> importMatch(
            @PathVariable Long userId,
            @Valid @RequestBody MatchImportRequest request) {
        MatchResponse match = matchService.importMatch(userId, request);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/user/{userId}/sync")
    public ResponseEntity<Void> syncMatches(@PathVariable Long userId) {
        matchService.syncUserMatches(userId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}
