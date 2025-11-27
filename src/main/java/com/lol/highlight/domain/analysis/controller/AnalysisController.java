package com.lol.highlight.domain.analysis.controller;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getAnalysis(@PathVariable Long id) {
        AnalysisResponse analysis = analysisService.getAnalysisById(id);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<AnalysisResponse> getAnalysisByMatch(@PathVariable Long matchId) {
        AnalysisResponse analysis = analysisService.getAnalysisByMatchId(matchId);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AnalysisResponse>> getUserAnalyses(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<AnalysisResponse> analyses = analysisService.getUserAnalyses(userId, pageable);
        return ResponseEntity.ok(analyses);
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> createAnalysis(
            @Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse analysis = analysisService.createAnalysis(request);
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<AnalysisResponse> regenerateAnalysis(@PathVariable Long id) {
        AnalysisResponse analysis = analysisService.regenerateAnalysis(id);
        return ResponseEntity.accepted().body(analysis);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        analysisService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }
}
