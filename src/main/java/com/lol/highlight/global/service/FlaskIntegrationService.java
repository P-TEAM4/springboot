package com.lol.highlight.global.service;

import com.lol.highlight.domain.analysis.entity.Analysis;
import com.lol.highlight.domain.analysis.entity.AnalysisStatus;
import com.lol.highlight.domain.analysis.repository.AnalysisRepository;
import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.entity.HighlightStatus;
import com.lol.highlight.domain.highlight.repository.HighlightRepository;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.global.client.FlaskApiClient;
import com.lol.highlight.global.client.dto.FlaskMatchAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlaskIntegrationService {

    private final FlaskApiClient flaskApiClient;
    private final AnalysisRepository analysisRepository;
    private final HighlightRepository highlightRepository;

    @Async
    @Transactional
    public void processAnalysisAsync(Analysis analysis, Match match) {
        try {
            log.info("Starting async analysis for match: {}", match.getMatchId());

            // Flask API 호출
            FlaskMatchAnalysisResponse response = flaskApiClient.analyzeMatch(
                    match.getMatchId(),
                    match.getUser().getSummonerName(),
                    match.getUser().getTagLine()
            );

            if (response != null && response.getError() == null) {
                // 분석 결과를 JSON으로 저장 (또는 별도 테이블에 저장)
                analysis.updateStatus(AnalysisStatus.COMPLETED);
                // TODO: response 데이터를 Analysis 엔티티에 저장
                log.info("Analysis completed for match: {}", match.getMatchId());
            } else {
                analysis.updateStatus(AnalysisStatus.FAILED);
                log.error("Analysis failed for match: {}. Error: {}",
                        match.getMatchId(),
                        response != null ? response.getError() : "Unknown error");
            }

            analysisRepository.save(analysis);

        } catch (Exception e) {
            log.error("Error during async analysis for match: {}", match.getMatchId(), e);
            analysis.updateStatus(AnalysisStatus.FAILED);
            analysisRepository.save(analysis);
        }
    }

    @Async
    @Transactional
    public void generateHighlightsAsync(Match match, List<Highlight> highlights) {
        try {
            log.info("Starting async highlight generation for match: {}", match.getMatchId());

            // Flask API 호출하여 하이라이트 추출
            FlaskMatchAnalysisResponse response = flaskApiClient.analyzeMatch(
                    match.getMatchId(),
                    match.getUser().getSummonerName(),
                    match.getUser().getTagLine()
            );

            if (response != null && response.getKeyMoments() != null) {
                // 키 모멘트를 기반으로 하이라이트 업데이트
                for (Highlight highlight : highlights) {
                    highlight.updateStatus(HighlightStatus.COMPLETED);
                    // TODO: 실제 비디오 URL 설정
                    highlightRepository.save(highlight);
                }
                log.info("Highlights generated for match: {}", match.getMatchId());
            } else {
                for (Highlight highlight : highlights) {
                    highlight.updateStatus(HighlightStatus.FAILED);
                    highlightRepository.save(highlight);
                }
                log.error("Highlight generation failed for match: {}", match.getMatchId());
            }

        } catch (Exception e) {
            log.error("Error during async highlight generation for match: {}", match.getMatchId(), e);
            for (Highlight highlight : highlights) {
                highlight.updateStatus(HighlightStatus.FAILED);
                highlightRepository.save(highlight);
            }
        }
    }

    @Async
    @Transactional
    public void generateAutoHighlightsAsync(Match match) {
        try {
            log.info("Starting async auto highlight generation for match: {}", match.getMatchId());

            // Flask API 호출
            FlaskMatchAnalysisResponse response = flaskApiClient.analyzeMatch(
                    match.getMatchId(),
                    match.getUser().getSummonerName(),
                    match.getUser().getTagLine()
            );

            if (response != null && response.getKeyMoments() != null) {
                // 키 모멘트를 기반으로 자동 하이라이트 생성
                for (FlaskMatchAnalysisResponse.KeyMoment moment : response.getKeyMoments()) {
                    Highlight highlight = Highlight.builder()
                            .match(match)
                            .title(moment.getType() + " - " + moment.getDescription())
                            .description(moment.getDescription())
                            .startTime(moment.getTimestamp() - 5) // 5초 전부터
                            .endTime(moment.getTimestamp() + 10) // 10초 후까지
                            .duration(15)
                            .status(HighlightStatus.PENDING)
                            .build();

                    highlightRepository.save(highlight);
                }
                log.info("Auto highlights created for match: {}", match.getMatchId());
            }

        } catch (Exception e) {
            log.error("Error during async auto highlight generation for match: {}", match.getMatchId(), e);
        }
    }
}
