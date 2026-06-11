package com.lol.highlight.domain.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.highlight.domain.analysis.dto.ai.AnalysisGenerateRequest;
import com.lol.highlight.domain.analysis.dto.ai.AnalysisGenerateResponse;
import com.lol.highlight.domain.analysis.dto.ai.GapAnalysisResponse;
import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Slf4j
@Service
public class AiAnalysisClient {

    private final RestTemplate restTemplate;
    private final AnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;

    private static final String ANALYSIS_GENERATE_ENDPOINT = "/api/v1/analysis/generate";

    public AiAnalysisClient(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            AnalysisRepository analysisRepository,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.analysisRepository = analysisRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void requestAnalysis(Long analysisId, AnalysisGenerateRequest request) {
        log.info("Requesting analysis for analysis ID: {}, matchId: {}", analysisId, request.getMatchId());

        try {
            AnalysisGenerateResponse response = restTemplate.postForObject(
                    ANALYSIS_GENERATE_ENDPOINT,
                    request,
                    AnalysisGenerateResponse.class
            );

            if (response != null) {
                updateAnalysisWithResult(analysisId, response);
                log.info("Analysis completed successfully for analysis ID: {}", analysisId);
            } else {
                markAnalysisFailed(analysisId, "AI 서버로부터 빈 응답을 받았습니다");
            }

        } catch (ResourceAccessException e) {
            handleConnectionError(analysisId, e);
        } catch (RestClientException e) {
            handleRestClientError(analysisId, e);
        } catch (Exception e) {
            handleUnexpectedError(analysisId, e);
        }
    }

    private void updateAnalysisWithResult(Long analysisId, AnalysisGenerateResponse response) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analysisId));

        GapAnalysisResponse gap = response.getGapAnalysis();

        String strengthAnalysis = gap != null && gap.getStrengths() != null
                ? String.join(" | ", gap.getStrengths()) : "";
        String weaknessAnalysis = gap != null && gap.getWeaknesses() != null
                ? String.join(" | ", gap.getWeaknesses()) : "";
        String improvementSuggestions = gap != null && gap.getRecommendations() != null
                ? String.join(" | ", gap.getRecommendations()) : "";

        Double impactScore = response.getImpactScore();

        // normalizedGaps(티어 대비 %)를 0-100 점수로 변환: 50 + (gap * 0.5), clamp 0-100
        Map<String, Double> normalizedGaps = gap != null ? gap.getNormalizedGaps() : null;
        Double teamFightScore = normalizeGap(normalizedGaps, "damage_share");
        Double farmingScore   = normalizeGap(normalizedGaps, "cs_per_min");
        Double visionScore    = normalizeGap(normalizedGaps, "vision_score_per_min");
        Double objectiveControlScore = normalizeGap(normalizedGaps, "gold_per_min");

        // overallScore는 FastAPI gap_analyzer가 이미 0-100으로 계산한 값 사용
        Double overallScore = gap != null ? gap.getOverallScore() : null;

        // aiModelData: 챔피언, 역할, 요약, 승률 변화 등 부가 정보
        String aiModelData = null;
        try {
            java.util.Map<String, Object> extra = new java.util.LinkedHashMap<>();
            extra.put("champion", response.getChampion() != null ? response.getChampion() : "");
            extra.put("role", response.getRole() != null ? response.getRole() : "");
            extra.put("win", response.getWin() != null ? response.getWin() : false);
            extra.put("summary", response.getSummary() != null ? response.getSummary() : "");
            extra.put("baselineProba", response.getBaselineProba() != null ? response.getBaselineProba() : 0.0);
            extra.put("predictedProba", response.getPredictedProba() != null ? response.getPredictedProba() : 0.0);
            extra.put("overallScore", overallScore != null ? overallScore : 0.0);
            extra.put("topFeatures", response.getTopFeatures() != null ? response.getTopFeatures() : java.util.List.of());
            // Gemini 경기분석 단계별 코칭
            GapAnalysisResponse gap2 = response.getGapAnalysis();
            if (gap2 != null) {
                extra.put("coachingSummary",    gap2.getCoachingSummary()    != null ? gap2.getCoachingSummary()    : "");
                extra.put("coachingEarlyGame",  gap2.getCoachingEarlyGame()  != null ? gap2.getCoachingEarlyGame()  : "");
                extra.put("coachingMidGame",    gap2.getCoachingMidGame()    != null ? gap2.getCoachingMidGame()    : "");
                extra.put("coachingLateGame",   gap2.getCoachingLateGame()   != null ? gap2.getCoachingLateGame()   : "");
                extra.put("coachingKeyPattern", gap2.getCoachingKeyPattern() != null ? gap2.getCoachingKeyPattern() : "");
            }
            aiModelData = objectMapper.writeValueAsString(extra);
        } catch (Exception e) {
            log.warn("Failed to serialize aiModelData for analysis ID: {}", analysisId, e);
        }

        analysis.updateAnalysisData(
                strengthAnalysis,
                weaknessAnalysis,
                improvementSuggestions,
                impactScore,
                teamFightScore,
                farmingScore,
                visionScore,
                objectiveControlScore
        );

        if (aiModelData != null) {
            analysis.updateAiModelData(aiModelData);
        }

        analysisRepository.save(analysis);
    }

    /** normalizedGaps의 % 값을 0-100 점수로 변환 (50 = 티어 평균) */
    private Double normalizeGap(Map<String, Double> normalizedGaps, String key) {
        if (normalizedGaps == null || !normalizedGaps.containsKey(key)) return null;
        double pct = normalizedGaps.get(key);
        return Math.max(0.0, Math.min(100.0, 50.0 + pct * 0.5));
    }

    private void handleConnectionError(Long analysisId, ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConnectException) {
            log.error("AI 서버 연결 실패. Analysis ID: {}", analysisId);
            markAnalysisFailed(analysisId, "AI 서버에 연결할 수 없습니다.");
        } else if (cause instanceof SocketTimeoutException) {
            log.error("AI 서버 응답 시간 초과. Analysis ID: {}", analysisId);
            markAnalysisFailed(analysisId, "AI 서버 응답 시간이 초과되었습니다.");
        } else {
            log.error("AI 서버 접근 오류. Analysis ID: {}", analysisId, e);
            markAnalysisFailed(analysisId, "AI 서버 접근 오류: " + e.getMessage());
        }
    }

    private void handleRestClientError(Long analysisId, RestClientException e) {
        log.error("AI 서버 요청 오류. Analysis ID: {}", analysisId, e);
        markAnalysisFailed(analysisId, "AI 서버 요청 오류: " + e.getMessage());
    }

    private void handleUnexpectedError(Long analysisId, Exception e) {
        log.error("AI 분석 중 예상치 못한 오류. Analysis ID: {}", analysisId, e);
        markAnalysisFailed(analysisId, "분석 중 오류 발생: " + e.getMessage());
    }

    private void markAnalysisFailed(Long analysisId, String errorMessage) {
        try {
            Analysis analysis = analysisRepository.findById(analysisId)
                    .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analysisId));
            analysis.updateStatus(AnalysisStatus.FAILED);
            analysisRepository.save(analysis);
            log.error("Marked analysis {} as FAILED: {}", analysisId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to mark analysis {} as FAILED", analysisId, e);
        }
    }
}
