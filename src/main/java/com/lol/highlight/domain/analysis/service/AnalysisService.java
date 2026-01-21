package com.lol.highlight.domain.analysis.service;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.dto.ai.GapAnalysisRequest;
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

/**
 * 경기 분석 서비스
 *
 * TODO: [FastAPI 연동]
 * AiAnalysisClient를 통해 FastAPI 서버와 통신합니다.
 * 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
 */
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

    /**
     * 매치 ID로 분석 정보를 조회합니다.
     * DB에 분석 정보가 없으면 자동으로 PENDING 상태로 생성하고 FastAPI에 분석 요청을 보냅니다.
     *
     * TODO: [FastAPI 연동]
     * - 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
     * - FastAPI 서버 배포 후 환경변수로 URL 설정 필요
     */
    @Transactional
    public AnalysisResponse getAnalysisByMatchId(Long matchId, String tier) {
        // 기존 분석이 있으면 반환
        return analysisRepository.findByMatchId(matchId)
                .map(AnalysisResponse::from)
                .orElseGet(() -> createAndRequestAnalysis(matchId, tier));
    }

    /**
     * 분석이 없을 경우 PENDING 상태로 생성하고 FastAPI에 요청
     */
    private AnalysisResponse createAndRequestAnalysis(Long matchId, String tier) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // PENDING 상태로 Analysis 생성
        Analysis analysis = Analysis.builder()
                .match(match)
                .status(AnalysisStatus.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);

        // FastAPI 서버에 비동기로 분석 요청
        requestAiAnalysis(analysis.getId(), match, tier);
        log.info("Analysis auto-created and requested for match ID: {}, analysis ID: {}",
                matchId, analysis.getId());

        return AnalysisResponse.from(analysis);
    }

    public Page<AnalysisResponse> getAnalysesByPuuid(String puuid, Pageable pageable) {
        return analysisRepository.findByPuuid(puuid, pageable)
                .map(AnalysisResponse::from);
    }

    @Transactional
    public AnalysisResponse createAnalysis(AnalysisCreateRequest request, String tier) {
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
        requestAiAnalysis(analysis.getId(), match, tier);
        log.info("Analysis creation initiated for match: {}", request.getMatchId());

        return AnalysisResponse.from(analysis);
    }

    /**
     * FastAPI 서버에 AI 분석 요청을 보냅니다.
     *
     * TODO: [FastAPI 연동]
     * - 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
     * - FastAPI 서버 배포 후 환경변수로 URL 설정 필요
     */
    private void requestAiAnalysis(Long analysisId, Match match, String tier) {
        String puuid = match.getPuuid();
        // tier가 null인 경우 기본값 사용
        String userTier = (tier != null) ? tier : "UNRANKED";

        GapAnalysisRequest aiRequest = GapAnalysisRequest.builder()
                .matchId(match.getMatchId())
                .puuid(puuid)
                .tier(userTier)
                .build();

        aiAnalysisClient.requestGapAnalysis(analysisId, aiRequest);
        log.info("AI analysis request sent for analysis ID: {}, matchId: {}, tier: {}", analysisId, match.getMatchId(), userTier);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        analysisRepository.delete(analysis);
    }

}
