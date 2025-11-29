package com.lol.highlight.domain.analysis.service;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.entity.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.service.FlaskIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MatchRepository matchRepository;
    private final FlaskIntegrationService flaskIntegrationService;

    public AnalysisResponse getAnalysisById(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    public AnalysisResponse getAnalysisByMatchId(Long matchId) {
        Analysis analysis = analysisRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    public Page<AnalysisResponse> getUserAnalyses(Long userId, Pageable pageable) {
        return analysisRepository.findByUserId(userId, pageable)
                .map(AnalysisResponse::from);
    }

    @Transactional
    public AnalysisResponse createAnalysis(AnalysisCreateRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (analysisRepository.existsByMatchId(request.getMatchId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Analysis already exists for this match");
        }

        Analysis analysis = Analysis.builder()
                .match(match)
                .status(AnalysisStatus.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);

        // 비동기로 AI 서버에 분석 요청
        flaskIntegrationService.processAnalysisAsync(analysis, match);
        log.info("Analysis creation initiated for match: {}", request.getMatchId());

        return AnalysisResponse.from(analysis);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        analysisRepository.delete(analysis);
    }

    @Transactional
    public AnalysisResponse regenerateAnalysis(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        analysis.updateStatus(AnalysisStatus.PENDING);

        // 비동기로 AI 서버에 재분석 요청
        flaskIntegrationService.processAnalysisAsync(analysis, analysis.getMatch());
        log.info("Analysis regeneration initiated for analysis: {}", id);

        return AnalysisResponse.from(analysis);
    }
}
