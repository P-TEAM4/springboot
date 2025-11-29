package com.lol.highlight.global.client;

import com.lol.highlight.global.client.dto.FlaskMatchAnalysisRequest;
import com.lol.highlight.global.client.dto.FlaskMatchAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class FlaskApiClient {

    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    public FlaskApiClient(
            RestTemplate restTemplate,
            @Value("${flask.api.base-url}") String flaskBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.flaskBaseUrl = flaskBaseUrl;
    }

    public FlaskMatchAnalysisResponse analyzeMatch(String matchId, String summonerName, String tagLine) {
        String url = flaskBaseUrl + "/api/v1/analyze/match";

        FlaskMatchAnalysisRequest request = FlaskMatchAnalysisRequest.builder()
                .matchId(matchId)
                .summonerName(summonerName)
                .tagLine(tagLine)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FlaskMatchAnalysisRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling Flask API to analyze match: {}", matchId);
            ResponseEntity<FlaskMatchAnalysisResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    FlaskMatchAnalysisResponse.class
            );

            log.info("Flask API response received for match: {}", matchId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to call Flask API for match analysis: {}", matchId, e);
            throw new RuntimeException("Flask API call failed", e);
        }
    }

    public boolean checkHealth() {
        String url = flaskBaseUrl + "/health";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Flask API health check failed", e);
            return false;
        }
    }
}
