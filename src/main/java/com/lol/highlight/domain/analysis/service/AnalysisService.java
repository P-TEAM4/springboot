package com.lol.highlight.domain.analysis.service;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.dto.ai.AnalysisGenerateRequest;
import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.enums.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.match.service.MatchService;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.external.riot.client.RiotApiClient;
import com.lol.highlight.global.external.riot.dto.RiotSummonerDto;
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
    private final MatchService matchService;
    private final RiotApiClient riotApiClient;
    private final AiAnalysisClient aiAnalysisClient;

    public AnalysisResponse getAnalysisById(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    /**
     * Riot 매치 ID로 분석 정보를 조회합니다.
     * DB에 분석 정보가 없으면 자동으로 PENDING 상태로 생성하고 FastAPI에 분석 요청을 보냅니다.
     * DB에 매치가 없으면 Riot API에서 가져옵니다.
     */
    @Transactional
    public AnalysisResponse getAnalysisByMatchId(String matchId, User user) {
        // 기존 분석이 있으면 반환
        return analysisRepository.findByMatch_MatchId(matchId)
                .map(AnalysisResponse::from)
                .orElseGet(() -> createAndRequestAnalysis(matchId, user));
    }

    /**
     * 분석이 없을 경우 PENDING 상태로 생성하고 FastAPI에 요청.
     * 매치가 DB에 없으면 Riot API에서 가져와 저장합니다.
     */
    private AnalysisResponse createAndRequestAnalysis(String matchId, User user) {
        Match match = getOrFetchMatch(matchId, user);

        // PENDING 상태로 Analysis 생성
        Analysis analysis = Analysis.builder()
                .match(match)
                .status(AnalysisStatus.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);

        // FastAPI 서버에 비동기로 분석 요청
        String tier = (user.getTier() != null) ? user.getTier() : "UNRANKED";
        requestAiAnalysis(analysis.getId(), match, tier, user.getSummonerName(), user.getTagLine());
        log.info("Analysis auto-created and requested for match ID: {}, analysis ID: {}",
                matchId, analysis.getId());

        return AnalysisResponse.from(analysis);
    }

    /**
     * DB에서 매치를 조회하고, 없으면 Riot API에서 가져와 저장합니다.
     */
    private Match getOrFetchMatch(String matchId, User user) {
        return matchRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    if (user.getSummonerName() == null || user.getTagLine() == null) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                                "Riot 계정이 연동되지 않았습니다. 먼저 Riot 계정을 연동해주세요.");
                    }

                    RiotSummonerDto summonerDto = riotApiClient.getSummonerByRiotId(
                            user.getSummonerName(), user.getTagLine());
                    String puuid = summonerDto.getPuuid();

                    log.info("Match {} not found in DB, fetching from Riot API for user: {}",
                            matchId, user.getEmail());
                    return matchService.fetchAndSaveMatch(puuid, matchId);
                });
    }

    public Page<AnalysisResponse> getAnalysesByPuuid(String puuid, Pageable pageable) {
        return analysisRepository.findByPuuid(puuid, pageable)
                .map(AnalysisResponse::from);
    }

    @Transactional
    public AnalysisResponse createAnalysis(AnalysisCreateRequest request, User user) {
        String matchId = request.getMatchId();

        Match match = getOrFetchMatch(matchId, user);

        // 기존 분석 있으면 삭제 후 재생성 (테스트용)
        analysisRepository.findByMatch_MatchId(matchId).ifPresent(existing -> {
            analysisRepository.delete(existing);
            analysisRepository.flush();
        });

        Analysis analysis = Analysis.builder()
                .match(match)
                .status(AnalysisStatus.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);

        // 비동기로 AI 서버에 분석 요청
        String tier = (user.getTier() != null) ? user.getTier() : "UNRANKED";
        requestAiAnalysis(analysis.getId(), match, tier, user.getSummonerName(), user.getTagLine());
        log.info("Analysis creation initiated for match: {}", matchId);

        return AnalysisResponse.from(analysis);
    }

    private void requestAiAnalysis(Long analysisId, Match match, String tier, String gameName, String tagLine) {
        AnalysisGenerateRequest aiRequest = AnalysisGenerateRequest.builder()
                .matchId(match.getMatchId())
                .puuid(match.getPuuid())
                .gameName(gameName)
                .tagLine(tagLine)
                .tier((tier != null) ? tier : "UNRANKED")
                .build();

        aiAnalysisClient.requestAnalysis(analysisId, aiRequest);
        log.info("AI analysis request sent for analysis ID: {}, matchId: {}", analysisId, match.getMatchId());
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        analysisRepository.delete(analysis);
    }

}
