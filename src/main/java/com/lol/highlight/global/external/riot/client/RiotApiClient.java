package com.lol.highlight.global.external.riot.client;

import com.lol.highlight.global.external.riot.dto.RiotLeagueDto;
import com.lol.highlight.global.external.riot.dto.RiotMatchDto;
import com.lol.highlight.global.external.riot.dto.RiotSummonerDto;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RiotApiClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url:https://kr.api.riotgames.com}")
    private String baseUrl;

    @Value("${riot.api.asia-url:https://asia.api.riotgames.com}")
    private String asiaUrl;

    public RiotSummonerDto getSummonerByRiotId(String gameName, String tagLine) {
        String url = String.format("%s/riot/account/v1/accounts/by-riot-id/%s/%s",
                asiaUrl, gameName, tagLine);

        log.info("Fetching summoner by Riot ID: {}#{}", gameName, tagLine);

        ResponseEntity<RiotSummonerDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(),
                RiotSummonerDto.class
        );

        return response.getBody();
    }

    public RiotSummonerDto getSummonerByPuuid(String puuid) {
        String url = String.format("%s/lol/summoner/v4/summoners/by-puuid/%s",
                baseUrl, puuid);

        log.info("Fetching summoner by PUUID: {}", puuid);

        ResponseEntity<RiotSummonerDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(),
                RiotSummonerDto.class
        );

        return response.getBody();
    }

    public List<RiotLeagueDto> getLeagueByPuuid(String puuid) {
        String url = String.format("%s/lol/league/v4/entries/by-puuid/%s",
                baseUrl, puuid);

        log.info("Fetching league by PUUID: {}", puuid);

        ResponseEntity<List<RiotLeagueDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(),
                new ParameterizedTypeReference<List<RiotLeagueDto>>() {}
        );

        return response.getBody();
    }

    public List<String> getMatchIdsByPuuid(String puuid, int count) {
        String url = String.format("%s/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=%d",
                asiaUrl, puuid, count);

        log.info("Fetching match IDs by PUUID: {}, count: {}", puuid, count);

        ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(),
                new ParameterizedTypeReference<List<String>>() {}
        );

        return response.getBody();
    }

    public RiotMatchDto getMatchById(String matchId) {
        String url = String.format("%s/lol/match/v5/matches/%s",
                asiaUrl, matchId);

        log.info("Fetching match by ID: {}", matchId);

        ResponseEntity<RiotMatchDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(),
                RiotMatchDto.class
        );

        return response.getBody();
    }

    private HttpEntity<Void> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        return new HttpEntity<>(headers);
    }
}
