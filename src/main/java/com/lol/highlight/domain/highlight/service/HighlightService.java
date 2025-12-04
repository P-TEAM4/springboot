package com.lol.highlight.domain.highlight.service;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HighlightService {

    private final HighlightRepository highlightRepository;
    private final MatchRepository matchRepository;

    public HighlightResponse getHighlightById(Long id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HIGHLIGHT_NOT_FOUND));
        return HighlightResponse.from(highlight);
    }

    public Page<HighlightResponse> getMatchHighlights(Long matchId, Pageable pageable) {
        return highlightRepository.findByMatchId(matchId, pageable)
                .map(HighlightResponse::from);
    }

    public Page<HighlightResponse> getUserHighlights(Long userId, Pageable pageable) {
        return highlightRepository.findByUserId(userId, pageable)
                .map(HighlightResponse::from);
    }

    @Transactional
    public HighlightResponse createHighlight(HighlightCreateRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
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

        // TODO: 비동기로 AI 서버에 하이라이트 영상 생성 요청
        log.info("Highlight creation initiated for match: {}", request.getMatchId());

        return HighlightResponse.from(highlight);
    }

    @Transactional
    public void generateAutoHighlights(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // TODO: AI 서버를 통해 자동으로 하이라이트 추출
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
