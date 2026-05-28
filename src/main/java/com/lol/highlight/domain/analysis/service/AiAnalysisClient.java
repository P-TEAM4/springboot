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
                ? String.join("\n", gap.getStrengths()) : "";
        String weaknessAnalysis = gap != null && gap.getWeaknesses() != null
                ? String.join("\n", gap.getWeaknesses()) : "";
        String improvementSuggestions = gap != null && gap.getRecommendations() != null
                ? String.join("\n", gap.getRecommendations()) : "";

        Double impactScore = response.getImpactScore();

        Map<String, Object> playerStats = response.getPlayerStats();
        Double teamFightScore = extractDouble(playerStats, "damage_share", "damage_dealt");
        Double farmingScore = extractDouble(playerStats, "cs_per_min", "cs");
        Double visionScore = extractDouble(playerStats, "vision_score", "vision_score_per_min");
        Double objectiveControlScore = extractDouble(playerStats, "gold_per_min", "gold");

        // summary + top_features를 aiModelData(JSON)에 저장
        String aiModelData = null;
        try {
            aiModelData = objectMapper.writeValueAsString(Map.of(
                    "summary", response.getSummary() != null ? response.getSummary() : "",
                    "topFeatures", response.getTopFeatures() != null ? response.getTopFeatures() : java.util.List.of(),
                    "champion", response.getChampion() != null ? response.getChampion() : "",
                    "role", response.getRole() != null ? response.getRole() : "",
                    "baselineProba", response.getBaselineProba() != null ? response.getBaselineProba() : 0.0,
                    "predictedProba", response.getPredictedProba() != null ? response.getPredictedProba() : 0.0
            ));
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

    private Double extractDouble(Map<String, Object> stats, String... keys) {
        if (stats == null) return null;
        for (String key : keys) {
            if (stats.containsKey(key)) {
                Object val = stats.get(key);
                if (val instanceof Number) return ((Number) val).doubleValue();
            }
        }
        return null;
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
