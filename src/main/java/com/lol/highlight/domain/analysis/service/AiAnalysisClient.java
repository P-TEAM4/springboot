package com.lol.highlight.domain.analysis.service;

import com.lol.highlight.domain.analysis.dto.ai.GapAnalysisRequest;
import com.lol.highlight.domain.analysis.dto.ai.GapAnalysisResponse;
import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisClient {

    @Qualifier("aiRestTemplate")
    private final RestTemplate restTemplate;
    private final AnalysisRepository analysisRepository;

    @Async
    @Transactional
    public void requestGapAnalysis(Long analysisId, GapAnalysisRequest request) {
        try {
            log.info("Requesting gap analysis for analysis ID: {}", analysisId);

            GapAnalysisResponse response = restTemplate.postForObject(
                    "/api/v1/analyze/gap",
                    request,
                    GapAnalysisResponse.class
            );

            if (response != null) {
                updateAnalysisWithGapResult(analysisId, response);
                log.info("Gap analysis completed successfully for analysis ID: {}", analysisId);
            } else {
                markAnalysisFailed(analysisId, "Empty response from AI server");
            }

        } catch (Exception e) {
            log.error("Failed to request gap analysis for analysis ID: {}", analysisId, e);
            markAnalysisFailed(analysisId, e.getMessage());
        }
    }

    private void updateAnalysisWithGapResult(Long analysisId, GapAnalysisResponse response) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analysisId));

        // Strengths, weaknesses, recommendations 변환
        String strengthAnalysis = response.getStrengths() != null
                ? String.join("\n", response.getStrengths())
                : "";

        String weaknessAnalysis = response.getWeaknesses() != null
                ? String.join("\n", response.getWeaknesses())
                : "";

        String improvementSuggestions = response.getRecommendations() != null
                ? String.join("\n", response.getRecommendations())
                : "";

        // Overall score를 impact score로 사용
        Double impactScore = response.getOverallScore();

        // player_stats에서 개별 스코어 추출
        // AI 서버가 반환하는 키 이름에 따라 조정 필요
        Double teamFightScore = extractScore(response.getPlayerStats(), "damage_share", "damage_dealt");
        Double farmingScore = extractScore(response.getPlayerStats(), "cs_per_min", "cs");
        Double visionScore = extractScore(response.getPlayerStats(), "vision_score", "vision_score_per_min");
        Double objectiveControlScore = extractScore(response.getPlayerStats(), "gold_per_min", "gold");

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

        analysisRepository.save(analysis);
    }

    private Double extractScore(java.util.Map<String, Double> stats, String... keys) {
        if (stats == null) {
            return null;
        }

        for (String key : keys) {
            if (stats.containsKey(key)) {
                return stats.get(key);
            }
        }
        return null;
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

    // TODO: 두 번째 AI 엔드포인트 구현
    // FastAPI 서버의 두 번째 분석 엔드포인트가 준비되면 아래 메서드를 구현하세요.
    // 현재 사용 가능한 엔드포인트:
    //   - /api/v1/analyze/match : 전체 매치 분석 (match_id, summoner_name, tag_line)
    //   - /api/v1/analyze/profile : 프로필 분석 (summoner_name, tag_line, recent_games)
    //
    // 예시:
    // @Async
    // @Transactional
    // public void requestMatchAnalysis(Long analysisId, MatchAnalysisRequest request) {
    //     try {
    //         log.info("Requesting match analysis for analysis ID: {}", analysisId);
    //
    //         MatchAnalysisResponse response = restTemplate.postForObject(
    //                 "/api/v1/analyze/match",
    //                 request,
    //                 MatchAnalysisResponse.class
    //         );
    //
    //         if (response != null) {
    //             // 응답 데이터를 Analysis 엔티티에 추가로 업데이트
    //             log.info("Match analysis completed successfully for analysis ID: {}", analysisId);
    //         }
    //     } catch (Exception e) {
    //         log.error("Failed to request match analysis for analysis ID: {}", analysisId, e);
    //     }
    // }
}
