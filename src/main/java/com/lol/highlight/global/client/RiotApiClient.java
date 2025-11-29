package com.lol.highlight.global.client;

import com.lol.highlight.global.client.dto.RiotMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class RiotApiClient {

    private final RestTemplate restTemplate;
    private final String riotApiKey;
    private final String riotBaseUrl;

    public RiotApiClient(
            RestTemplate restTemplate,
            @Value("${riot.api.key}") String riotApiKey,
            @Value("${riot.api.base-url}") String riotBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.riotApiKey = riotApiKey;
        this.riotBaseUrl = riotBaseUrl;
    }

    public RiotMatchResponse getMatchDetails(String matchId) {
        String url = String.format("%s/lol/match/v5/matches/%s", riotBaseUrl, matchId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("Calling Riot API to get match details: {}", matchId);
            ResponseEntity<RiotMatchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotMatchResponse.class
            );

            log.info("Riot API response received for match: {}", matchId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to call Riot API for match: {}", matchId, e);
            throw new RuntimeException("Riot API call failed", e);
        }
    }

    public List<String> getRecentMatchIds(String puuid, int count) {
        String url = String.format("%s/lol/match/v5/matches/by-puuid/%s/ids?count=%d",
                riotBaseUrl, puuid, count);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("Calling Riot API to get recent matches for puuid: {}", puuid);
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            log.info("Riot API returned {} matches", response.getBody().size());
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to call Riot API for recent matches: {}", puuid, e);
            throw new RuntimeException("Riot API call failed", e);
        }
    }
}
