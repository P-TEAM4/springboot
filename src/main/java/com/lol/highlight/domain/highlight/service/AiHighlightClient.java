package com.lol.highlight.domain.highlight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateRequest;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateResponse;
import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.enums.HighlightType;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

@Slf4j
@Service
public class AiHighlightClient {

    private final RestTemplate restTemplate;
    private final HighlightRepository highlightRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    private static final String HIGHLIGHT_GENERATE_ENDPOINT = "/api/v1/highlights/generate";

    public AiHighlightClient(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            HighlightRepository highlightRepository,
            MatchRepository matchRepository,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.highlightRepository = highlightRepository;
        this.matchRepository = matchRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 하이라이트 영상 생성 요청 (비동기)
     * 영상 전체를 FastAPI로 전송하면 highlights/mistakes 클립 목록을 반환.
     * 초기 PENDING 레코드를 삭제하고 클립별로 새 Highlight를 생성.
     */
    @Async
    @Transactional
    public void requestHighlightGeneration(Long pendingHighlightId, byte[] videoBytes,
                                           String filename, HighlightGenerateRequest request) {
        log.info("Requesting highlight generation for match: {}, game: {}#{}",
                request.getMatchId(), request.getGameName(), request.getTagLine());

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", new ByteArrayResource(videoBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            body.add("match_id", request.getMatchId());
            body.add("game_name", request.getGameName());
            body.add("tag_line", request.getTagLine());
            body.add("top_highlights", String.valueOf(request.getTopHighlights()));
            body.add("top_mistakes", String.valueOf(request.getTopMistakes()));
            body.add("game_start_offset", String.valueOf(request.getGameStartOffset()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            HighlightGenerateResponse response = restTemplate.postForObject(
                    HIGHLIGHT_GENERATE_ENDPOINT,
                    requestEntity,
                    HighlightGenerateResponse.class
            );

            if (response != null && response.getHighlights() != null) {
                Match match = matchRepository.findByMatchId(request.getMatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Match not found: " + request.getMatchId()));

                // 기존 COMPLETED 클립 삭제 후 재생성 (덮어쓰기)
                List<com.lol.highlight.domain.highlight.entity.Highlight> existing =
                        highlightRepository.findByMatch_MatchIdAndStatus(request.getMatchId(), HighlightStatus.COMPLETED)
                                .stream()
                                .filter(h -> h.getVideoUrl() != null && !h.getVideoUrl().isEmpty())
                                .toList();
                if (!existing.isEmpty()) {
                    highlightRepository.deleteAll(existing);
                    log.info("Deleted {} existing completed highlights for match: {}", existing.size(), request.getMatchId());
                }
                createHighlightsFromClips(match, response.getHighlights(), false);
                createHighlightsFromClips(match, response.getMistakes(), true);

                // PENDING → COMPLETED (프론트 폴링이 성공하도록)
                highlightRepository.findByMatch_MatchIdAndStatus(request.getMatchId(), HighlightStatus.PENDING)
                        .forEach(h -> {
                            h.updateStatus(HighlightStatus.COMPLETED);
                            highlightRepository.save(h);
                        });

                log.info("Highlight generation completed. highlights={}, mistakes={} for match: {}",
                        response.getHighlights().size(),
                        response.getMistakes() != null ? response.getMistakes().size() : 0,
                        request.getMatchId());
            } else {
                markHighlightFailed(pendingHighlightId, "AI 서버로부터 빈 응답을 받았습니다");
            }

        } catch (ResourceAccessException e) {
            handleConnectionError(pendingHighlightId, e);
        } catch (RestClientException e) {
            handleRestClientError(pendingHighlightId, e);
        } catch (Exception e) {
            handleUnexpectedError(pendingHighlightId, e);
        }
    }

    @Async
    @Transactional
    public void requestHighlightGenerationWithoutVideo(Long pendingHighlightId,
                                                        HighlightGenerateRequest request) {
        log.info("Requesting auto highlight generation (no video) for match: {}, game: {}#{}",
                request.getMatchId(), request.getGameName(), request.getTagLine());

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", new ByteArrayResource(new byte[0]) {
                @Override
                public String getFilename() { return "empty.mp4"; }
            });
            body.add("match_id", request.getMatchId());
            body.add("game_name", request.getGameName());
            body.add("tag_line", request.getTagLine());
            body.add("top_highlights", String.valueOf(request.getTopHighlights()));
            body.add("top_mistakes", String.valueOf(request.getTopMistakes()));
            body.add("game_start_offset", String.valueOf(request.getGameStartOffset()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            HighlightGenerateResponse response = restTemplate.postForObject(
                    HIGHLIGHT_GENERATE_ENDPOINT,
                    requestEntity,
                    HighlightGenerateResponse.class
            );

            if (response != null && response.getHighlights() != null) {
                Match match = matchRepository.findByMatchId(request.getMatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Match not found: " + request.getMatchId()));

                highlightRepository.deleteAllByMatch_MatchId(request.getMatchId());

                createHighlightsFromClips(match, response.getHighlights(), false);
                createHighlightsFromClips(match, response.getMistakes(), true);

                log.info("Auto highlight generation completed. highlights={}, mistakes={} for match: {}",
                        response.getHighlights().size(),
                        response.getMistakes() != null ? response.getMistakes().size() : 0,
                        request.getMatchId());
            } else {
                markHighlightFailed(pendingHighlightId, "AI 서버로부터 빈 응답을 받았습니다");
            }

        } catch (ResourceAccessException e) {
            handleConnectionError(pendingHighlightId, e);
        } catch (RestClientException e) {
            handleRestClientError(pendingHighlightId, e);
        } catch (Exception e) {
            handleUnexpectedError(pendingHighlightId, e);
        }
    }

    private void createHighlightsFromClips(Match match,
                                            List<HighlightGenerateResponse.ClipInfo> clips,
                                            boolean isMistake) {
        if (clips == null) return;

        for (HighlightGenerateResponse.ClipInfo clip : clips) {
            Integer startTime = clip.getTimestamp() != null ? clip.getTimestamp().intValue() : null;
            String prefix = isMistake ? "[실수] " : "[하이라이트] ";

            String eventData = null;
            try {
                java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
                data.put("baseImportance", clip.getBaseImportance());
                data.put("impactScore", clip.getImpactScore());
                data.put("combinedImportance", clip.getCombinedImportance());
                data.put("eventType", clip.getType());
                if (clip.getDetails() != null) data.put("details", clip.getDetails());
                eventData = objectMapper.writeValueAsString(data);
            } catch (Exception e) {
                log.warn("Failed to serialize eventData for clip: {}", clip.getDescription());
            }

            Highlight highlight = Highlight.builder()
                    .match(match)
                    .title(prefix + clip.getDescription())
                    .description(clip.getImpactDescription())
                    .videoUrl(clip.getClipPath())
                    .startTime(startTime)
                    .duration(15)
                    .type(resolveHighlightType(clip.getType()))
                    .status(HighlightStatus.COMPLETED)
                    .eventData(eventData)
                    .coaching(clip.getCoaching())
                    .build();

            highlightRepository.save(highlight);
        }
    }

    private HighlightType resolveHighlightType(String type) {
        if (type == null) return HighlightType.CUSTOM;
        return switch (type.toUpperCase()) {
            case "CHAMPION_KILL", "KILL" -> HighlightType.KILL;
            case "MULTI_KILL" -> HighlightType.MULTI_KILL;
            case "PENTAKILL" -> HighlightType.PENTAKILL;
            case "BARON" -> HighlightType.BARON;
            case "DRAGON" -> HighlightType.DRAGON;
            case "BUILDING_KILL", "TOWER_DESTROY", "TOWER" -> HighlightType.TOWER_DESTROY;
            case "TEAM_FIGHT" -> HighlightType.TEAM_FIGHT;
            default -> HighlightType.CUSTOM;
        };
    }

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

    private void handleConnectionError(Long highlightId, ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConnectException) {
            log.error("AI 서버 연결 실패. Highlight ID: {}", highlightId);
            markHighlightFailed(highlightId, "AI 서버에 연결할 수 없습니다.");
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
        log.error("하이라이트 생성 중 예상치 못한 오류. Highlight ID: {}", highlightId, e);
        markHighlightFailed(highlightId, "하이라이트 생성 중 오류: " + e.getMessage());
    }
}
