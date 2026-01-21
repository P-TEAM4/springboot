package com.lol.highlight.domain.highlight.service;

import com.lol.highlight.domain.highlight.dto.ai.AutoHighlightRequest;
import com.lol.highlight.domain.highlight.dto.ai.AutoHighlightResponse;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateRequest;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateResponse;
import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
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
import java.util.List;

/**
 * FastAPI AI 서버와 통신하여 하이라이트를 생성하는 클라이언트
 *
 * TODO: [FastAPI 연동]
 * 현재 임시 URL(localhost:8000)로 설정되어 있습니다.
 * FastAPI 서버 엔드포인트:
 *   - POST /api/v1/highlight/generate : 하이라이트 영상 생성 (matchId, startTime, endTime, ...)
 *   - POST /api/v1/highlight/auto-generate : AI 자동 하이라이트 추출 (matchId)
 */
@Slf4j
@Service
public class AiHighlightClient {

    private final RestTemplate restTemplate;
    private final HighlightRepository highlightRepository;
    private final MatchRepository matchRepository;

    /**
     * TODO: [임시 엔드포인트]
     * FastAPI 서버 개발 완료 후 실제 엔드포인트로 변경
     */
    private static final String HIGHLIGHT_GENERATE_ENDPOINT = "/api/v1/highlight/generate";
    private static final String AUTO_HIGHLIGHT_ENDPOINT = "/api/v1/highlight/auto-generate";

    public AiHighlightClient(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            HighlightRepository highlightRepository,
            MatchRepository matchRepository) {
        this.restTemplate = restTemplate;
        this.highlightRepository = highlightRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * 하이라이트 영상 생성 요청 (비동기)
     *
     * @param highlightId 하이라이트 ID
     * @param request 영상 생성 요청 데이터
     */
    @Async
    @Transactional
    public void requestHighlightGeneration(Long highlightId, HighlightGenerateRequest request) {
        log.info("Requesting highlight generation for highlight ID: {}, matchId: {}",
                highlightId, request.getMatchId());

        try {
            HighlightGenerateResponse response = restTemplate.postForObject(
                    HIGHLIGHT_GENERATE_ENDPOINT,
                    request,
                    HighlightGenerateResponse.class
            );

            if (response != null && response.getVideoUrl() != null) {
                updateHighlightWithVideo(highlightId, response);
                log.info("Highlight generation completed successfully for highlight ID: {}", highlightId);
            } else {
                markHighlightFailed(highlightId, "AI 서버로부터 빈 응답을 받았습니다");
            }

        } catch (ResourceAccessException e) {
            handleConnectionError(highlightId, e);
        } catch (RestClientException e) {
            handleRestClientError(highlightId, e);
        } catch (Exception e) {
            handleUnexpectedError(highlightId, e);
        }
    }

    /**
     * AI 자동 하이라이트 추출 요청 (비동기)
     *
     * @param matchId 매치 ID (DB ID)
     */
    @Async
    @Transactional
    public void requestAutoHighlightGeneration(Long matchId) {
        log.info("Requesting auto highlight generation for match ID: {}", matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        AutoHighlightRequest request = AutoHighlightRequest.builder()
                .matchId(match.getMatchId())
                .puuid(match.getPuuid())
                .build();

        try {
            AutoHighlightResponse response = restTemplate.postForObject(
                    AUTO_HIGHLIGHT_ENDPOINT,
                    request,
                    AutoHighlightResponse.class
            );

            if (response != null && response.getHighlights() != null) {
                createHighlightsFromResponse(match, response);
                log.info("Auto highlight generation completed. Created {} highlights for match ID: {}",
                        response.getHighlights().size(), matchId);
            } else {
                log.warn("AI 서버로부터 빈 응답을 받았습니다. Match ID: {}", matchId);
            }

        } catch (ResourceAccessException e) {
            handleConnectionErrorForMatch(matchId, e);
        } catch (RestClientException e) {
            handleRestClientErrorForMatch(matchId, e);
        } catch (Exception e) {
            handleUnexpectedErrorForMatch(matchId, e);
        }
    }

    /**
     * AI 응답을 기반으로 하이라이트 엔티티들 생성
     */
    private void createHighlightsFromResponse(Match match, AutoHighlightResponse response) {
        List<AutoHighlightResponse.HighlightData> highlightDataList = response.getHighlights();

        for (AutoHighlightResponse.HighlightData data : highlightDataList) {
            Highlight highlight = Highlight.builder()
                    .match(match)
                    .title(data.getTitle())
                    .description(data.getDescription())
                    .startTime(data.getStartTime())
                    .endTime(data.getEndTime())
                    .duration(data.getEndTime() - data.getStartTime())
                    .type(data.getType())
                    .status(HighlightStatus.PENDING)
                    .build();

            Highlight savedHighlight = highlightRepository.save(highlight);

            // 각 하이라이트에 대해 영상 생성 요청
            HighlightGenerateRequest generateRequest = HighlightGenerateRequest.builder()
                    .matchId(match.getMatchId())
                    .highlightId(savedHighlight.getId())
                    .startTime(data.getStartTime())
                    .endTime(data.getEndTime())
                    .build();

            requestHighlightGeneration(savedHighlight.getId(), generateRequest);
        }
    }

    /**
     * 하이라이트 영상 URL 업데이트
     */
    private void updateHighlightWithVideo(Long highlightId, HighlightGenerateResponse response) {
        Highlight highlight = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new IllegalArgumentException("Highlight not found: " + highlightId));

        highlight.updateVideoUrl(response.getVideoUrl());
        highlight.updateThumbnailUrl(response.getThumbnailUrl());
        highlight.updateStatus(HighlightStatus.COMPLETED);

        highlightRepository.save(highlight);
    }

    /**
     * 하이라이트 실패 상태로 변경
     */
    private void markHighlightFailed(Long highlightId, String errorMessage) {
        try {
            Highlight highlight = highlightRepository.findById(highlightId)
                    .orElseThrow(() -> new IllegalArgumentException("Highlight not found: " + highlightId));
            highlight.updateStatus(HighlightStatus.FAILED);
            highlightRepository.save(highlight);
            log.error("Marked highlight {} as FAILED: {}", highlightId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to mark highlight {} as FAILED", highlightId, e);
        }
    }

    // ===== 에러 핸들러 (단일 하이라이트용) =====

    private void handleConnectionError(Long highlightId, ResourceAccessException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ConnectException) {
            log.error("AI 서버 연결 실패 - 서버가 실행 중인지 확인하세요. Highlight ID: {}", highlightId);
            markHighlightFailed(highlightId, "AI 서버에 연결할 수 없습니다. 서버가 실행 중인지 확인하세요.");
        } else if (cause instanceof SocketTimeoutException) {
            log.error("AI 서버 응답 시간 초과. Highlight ID: {}", highlightId);
            markHighlightFailed(highlightId, "AI 서버 응답 시간이 초과되었습니다.");
        } else {
            log.error("AI 서버 접근 오류. Highlight ID: {}", highlightId, e);
            markHighlightFailed(highlightId, "AI 서버 접근 오류: " + e.getMessage());
        }
    }

    private void handleRestClientError(Long highlightId, RestClientException e) {
        log.error("AI 서버 요청 오류. Highlight ID: {}", highlightId, e);
        markHighlightFailed(highlightId, "AI 서버 요청 오류: " + e.getMessage());
    }

    private void handleUnexpectedError(Long highlightId, Exception e) {
        log.error("하이라이트 생성 중 예상치 못한 오류 발생. Highlight ID: {}", highlightId, e);
        markHighlightFailed(highlightId, "하이라이트 생성 중 오류 발생: " + e.getMessage());
    }

    // ===== 에러 핸들러 (매치 전체 자동 생성용) =====

    private void handleConnectionErrorForMatch(Long matchId, ResourceAccessException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ConnectException) {
            log.error("AI 서버 연결 실패 - 서버가 실행 중인지 확인하세요. Match ID: {}", matchId);
        } else if (cause instanceof SocketTimeoutException) {
            log.error("AI 서버 응답 시간 초과. Match ID: {}", matchId);
        } else {
            log.error("AI 서버 접근 오류. Match ID: {}", matchId, e);
        }
    }

    private void handleRestClientErrorForMatch(Long matchId, RestClientException e) {
        log.error("AI 서버 요청 오류. Match ID: {}", matchId, e);
    }

    private void handleUnexpectedErrorForMatch(Long matchId, Exception e) {
        log.error("자동 하이라이트 생성 중 예상치 못한 오류 발생. Match ID: {}", matchId, e);
    }
}
