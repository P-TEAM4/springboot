package com.lol.highlight.domain.highlight.service;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateRequest;
import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
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
 * 하이라이트 관리 서비스
 *
 * TODO: [FastAPI 연동]
 * AiHighlightClient를 통해 FastAPI 서버와 통신합니다.
 * 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HighlightService {

    private final HighlightRepository highlightRepository;
    private final MatchRepository matchRepository;
    private final AiHighlightClient aiHighlightClient;

    public HighlightResponse getHighlightById(Long id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HIGHLIGHT_NOT_FOUND));
        return HighlightResponse.from(highlight);
    }

    public Page<HighlightResponse> getMatchHighlights(String matchId, Pageable pageable) {
        return highlightRepository.findByMatch_MatchId(matchId, pageable)
                .map(HighlightResponse::from);
    }

    public Page<HighlightResponse> getHighlightsByPuuid(String puuid, Pageable pageable) {
        return highlightRepository.findByPuuid(puuid, pageable)
                .map(HighlightResponse::from);
    }

    /**
     * 하이라이트를 생성합니다.
     * PENDING 상태로 저장 후 FastAPI 서버에 비동기 영상 생성 요청을 보냅니다.
     *
     * TODO: [FastAPI 연동]
     * - 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
     * - FastAPI 서버 배포 후 환경변수로 URL 설정 필요
     */
    @Transactional
    public HighlightResponse createHighlight(HighlightCreateRequest request) {
        Match match = matchRepository.findByMatchId(request.getMatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        Integer duration = request.getEndTime() - request.getStartTime();

        Highlight highlight = Highlight.builder()
                .match(match)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .duration(duration)
                .type(request.getType())
                .status(HighlightStatus.PENDING)
                .build();

        highlight = highlightRepository.save(highlight);

        // FastAPI 서버에 비동기로 하이라이트 영상 생성 요청
        HighlightGenerateRequest generateRequest = HighlightGenerateRequest.builder()
                .matchId(match.getMatchId())
                .highlightId(highlight.getId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .puuid(match.getPuuid())
                .build();

        aiHighlightClient.requestHighlightGeneration(highlight.getId(), generateRequest);
        log.info("Highlight creation initiated for match: {}, highlightId: {}",
                request.getMatchId(), highlight.getId());

        return HighlightResponse.from(highlight);
    }

    /**
     * AI를 통해 매치의 주요 장면을 자동으로 분석하여 하이라이트를 생성합니다.
     *
     * TODO: [FastAPI 연동]
     * - 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
     * - FastAPI 서버 배포 후 환경변수로 URL 설정 필요
     * - AI가 킬/타워/오브젝트 등 주요 장면을 자동 추출합니다.
     */
    @Transactional
    public void generateAutoHighlights(String matchId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // FastAPI 서버에 비동기로 자동 하이라이트 추출 요청
        aiHighlightClient.requestAutoHighlightGeneration(matchId);
        log.info("Auto highlight generation initiated for match: {}", matchId);
    }

    @Transactional
    public void deleteHighlight(Long id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HIGHLIGHT_NOT_FOUND));
        highlightRepository.delete(highlight);
    }

    @Transactional
    public HighlightResponse incrementViewCount(Long id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HIGHLIGHT_NOT_FOUND));

        highlight.incrementViewCount();

        return HighlightResponse.from(highlight);
    }
}
