package com.lol.highlight.domain.highlight.controller;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
import com.lol.highlight.domain.highlight.service.HighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/highlights")
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @GetMapping("/{id}")
    public ResponseEntity<HighlightResponse> getHighlight(@PathVariable Long id) {
        HighlightResponse highlight = highlightService.getHighlightById(id);
        return ResponseEntity.ok(highlight);
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<Page<HighlightResponse>> getMatchHighlights(
            @PathVariable Long matchId,
            Pageable pageable) {
        Page<HighlightResponse> highlights = highlightService.getMatchHighlights(matchId, pageable);
        return ResponseEntity.ok(highlights);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<HighlightResponse>> getUserHighlights(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<HighlightResponse> highlights = highlightService.getUserHighlights(userId, pageable);
        return ResponseEntity.ok(highlights);
    }

    @PostMapping
    public ResponseEntity<HighlightResponse> createHighlight(
            @Valid @RequestBody HighlightCreateRequest request) {
        HighlightResponse highlight = highlightService.createHighlight(request);
        return ResponseEntity.ok(highlight);
    }

    @PostMapping("/match/{matchId}/auto-generate")
    public ResponseEntity<Void> generateAutoHighlights(@PathVariable Long matchId) {
        highlightService.generateAutoHighlights(matchId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<HighlightResponse> incrementViewCount(@PathVariable Long id) {
        HighlightResponse highlight = highlightService.incrementViewCount(id);
        return ResponseEntity.ok(highlight);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHighlight(@PathVariable Long id) {
        highlightService.deleteHighlight(id);
        return ResponseEntity.noContent().build();
    }
}
