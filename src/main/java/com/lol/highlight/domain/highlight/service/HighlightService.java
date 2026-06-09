package com.lol.highlight.domain.highlight.service;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
import com.lol.highlight.domain.highlight.dto.ai.HighlightGenerateRequest;
import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.match.service.MatchService;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HighlightService {

    private final HighlightRepository highlightRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
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
     * PENDING 상태로 저장 후 영상과 함께 FastAPI에 비동기로 전송.
     * FastAPI 응답의 클립 목록으로 PENDING 레코드를 대체합니다.
     */
    @Transactional
    public HighlightResponse createHighlight(HighlightCreateRequest request,
                                              MultipartFile video,
                                              User user) {
        if (user.getSummonerName() == null || user.getTagLine() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "Riot 계정이 연동되지 않았습니다. 먼저 Riot 계정을 연동해주세요.");
        }

        Match match;
        try {
            match = matchService.fetchAndSaveMatch(user.getPuuid(), request.getMatchId());
        } catch (Exception e) {
            log.warn("Failed to fetch match {} from Riot API, trying local DB: {}", request.getMatchId(), e.getMessage());
            match = matchRepository.findByMatchId(request.getMatchId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        }

        Highlight pending = Highlight.builder()
                .match(match)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .status(HighlightStatus.PENDING)
                .build();

        pending = highlightRepository.save(pending);

        try {
            byte[] videoBytes = video.getBytes();
            String filename = video.getOriginalFilename() != null
                    ? video.getOriginalFilename() : "video.mp4";

            HighlightGenerateRequest generateRequest = HighlightGenerateRequest.builder()
                    .matchId(match.getMatchId())
                    .gameName(user.getSummonerName())
                    .tagLine(user.getTagLine())
                    .gameStartOffset(request.getGameStartOffset() != null ? request.getGameStartOffset() : 0.0)
                    .build();

            aiHighlightClient.requestHighlightGeneration(pending.getId(), videoBytes, filename, generateRequest);
            log.info("Highlight generation requested for match: {}, pendingId: {}",
                    request.getMatchId(), pending.getId());

        } catch (IOException e) {
            pending.updateStatus(HighlightStatus.FAILED);
            highlightRepository.save(pending);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "영상 파일을 읽을 수 없습니다.");
        }

        return HighlightResponse.from(pending);
    }

    @Transactional
    public void autoGenerateHighlights(String matchId, User user) {
        if (user.getSummonerName() == null || user.getTagLine() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "Riot 계정이 연동되지 않았습니다. 먼저 Riot 계정을 연동해주세요.");
        }

        Match match;
        try {
            match = matchService.fetchAndSaveMatch(user.getPuuid(), matchId);
        } catch (Exception e) {
            log.warn("Failed to fetch match {} from Riot API, trying local DB: {}", matchId, e.getMessage());
            match = matchRepository.findByMatchId(matchId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        }

        Highlight pending = Highlight.builder()
                .match(match)
                .title(matchId + " 하이라이트")
                .type(com.lol.highlight.domain.highlight.enums.HighlightType.CUSTOM)
                .status(HighlightStatus.PENDING)
                .build();
        pending = highlightRepository.save(pending);

        HighlightGenerateRequest generateRequest = HighlightGenerateRequest.builder()
                .matchId(matchId)
                .gameName(user.getSummonerName())
                .tagLine(user.getTagLine())
                .build();

        aiHighlightClient.requestHighlightGenerationWithoutVideo(pending.getId(), generateRequest);
        log.info("Auto highlight generation requested for match: {}, pendingId: {}", matchId, pending.getId());
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
