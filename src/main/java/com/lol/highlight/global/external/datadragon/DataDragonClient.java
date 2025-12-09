package com.lol.highlight.global.external.datadragon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataDragonClient {

    private final RestTemplate restTemplate;

    @Value("${datadragon.base-url:https://ddragon.leagueoflegends.com/cdn}")
    private String baseUrl;

    @Value("${datadragon.language:ko_KR}")
    private String language;

    /**
     * 특정 패치 버전의 모든 아이템 정보 조회
     */
    public Map<String, Object> getItems(String patchVersion) {
        String url = String.format("%s/%s/data/%s/item.json", baseUrl, patchVersion, language);
        log.info("Fetching items from Data Dragon: patch={}", patchVersion);

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch items from Data Dragon: patch={}", patchVersion, e);
            throw new RuntimeException("Failed to fetch items from Data Dragon", e);
        }
    }

    /**
     * 아이템 이미지 URL 생성
     */
    public String getItemImageUrl(String patchVersion, String imageName) {
        return String.format("%s/%s/img/item/%s", baseUrl, patchVersion, imageName);
    }

    /**
     * 현재 최신 패치 버전 조회
     */
    public String getLatestVersion() {
        String url = "https://ddragon.leagueoflegends.com/api/versions.json";
        log.info("Fetching latest version from Data Dragon");

        try {
            String[] versions = restTemplate.getForObject(url, String[].class);
            return versions != null && versions.length > 0 ? versions[0] : null;
        } catch (Exception e) {
            log.error("Failed to fetch latest version from Data Dragon", e);
            throw new RuntimeException("Failed to fetch latest version from Data Dragon", e);
        }
    }
}
