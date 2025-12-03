package com.lol.highlight.domain.analysis.service;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
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
    private final AiAnalysisClient aiAnalysisClient;

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
        requestAiAnalysis(analysis.getId(), match);
        log.info("Analysis creation initiated for match: {}", request.getMatchId());

        return AnalysisResponse.from(analysis);
    }

    private void requestAiAnalysis(Long analysisId, Match match) {
        // TODO: User 엔티티에 puuid와 tier 필드 추가 필요
        // 현재는 matchId만으로 요청하며, AI 서버가 match 데이터로부터 puuid를 추출할 수 있어야 함
        // 또는 Match 엔티티에 puuid 필드를 추가하거나, User 엔티티와의 관계를 통해 가져와야 함

        String puuid = "TEMP_PUUID"; // TODO: match.getUser().getPuuid() 또는 match.getPuuid()로 변경
        String tier = "GOLD"; // TODO: match.getUser().getTier()로 변경

        com.lol.highlight.domain.analysis.dto.ai.GapAnalysisRequest aiRequest =
            com.lol.highlight.domain.analysis.dto.ai.GapAnalysisRequest.builder()
                .matchId(match.getMatchId())
                .puuid(puuid)
                .tier(tier)
                .build();

        aiAnalysisClient.requestGapAnalysis(analysisId, aiRequest);

        // TODO: 두 번째 AI 엔드포인트 호출 (예: /api/v1/analyze/match)
        // aiAnalysisClient.requestMatchAnalysis(analysisId, matchAnalysisRequest);
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

        // TODO: 비동기로 AI 서버에 재분석 요청
        log.info("Analysis regeneration initiated for analysis: {}", id);

        return AnalysisResponse.from(analysis);
    }
}
