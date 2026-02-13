package com.lol.highlight.global.external.datadragon;

import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.global.external.datadragon.entity.DataDragonVersion;
import com.lol.highlight.global.external.datadragon.repository.DataDragonVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataDragonService {

    private final DataDragonClient dataDragonClient;
    private final DataDragonVersionRepository versionRepository;

    /**
     * 아이템 ID 리스트를 받아서 아이템 정보 리스트 반환
     */
    public List<MatchDetailResponse.ItemInfo> getItemsInfo(List<Integer> itemIds, String gameVersion) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 게임 버전에서 패치 버전 추출 (14.23.1.12345 -> 14.23)
        String patchVersion = extractPatchVersion(gameVersion);

        List<MatchDetailResponse.ItemInfo> itemInfoList = new ArrayList<>();
        Map<String, Object> itemsData = getItemsData(patchVersion);

        if (itemsData == null || !itemsData.containsKey("data")) {
            log.warn("No items data available for version: {}", patchVersion);
            return itemInfoList;
        }

        Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) itemsData.get("data");

        for (Integer itemId : itemIds) {
            if (itemId == null || itemId == 0) {
                // 빈 아이템 슬롯
                continue;
            }

            String itemIdStr = String.valueOf(itemId);
            if (items.containsKey(itemIdStr)) {
                Map<String, Object> itemData = items.get(itemIdStr);

                MatchDetailResponse.ItemInfo itemInfo = MatchDetailResponse.ItemInfo.builder()
                        .itemId(itemId)
                        .itemName((String) itemData.get("name"))
                        .itemDescription(extractPlainText((String) itemData.get("description")))
                        .itemImageUrl(dataDragonClient.getItemImageUrl(patchVersion,
                                ((Map<String, String>) itemData.get("image")).get("full")))
                        .build();

                itemInfoList.add(itemInfo);
            } else {
                log.warn("Item not found in Data Dragon: itemId={}, version={}", itemId, patchVersion);
            }
        }

        return itemInfoList;
    }

    /**
     * 특정 버전의 아이템 데이터를 캐싱하여 조회
     */
    @Cacheable(value = "itemsData", key = "#patchVersion")
    public Map<String, Object> getItemsData(String patchVersion) {
        try {
            log.info("Fetching items data from Data Dragon for version: {}", patchVersion);
            return dataDragonClient.getItems(patchVersion);
        } catch (Exception e) {
            log.error("Failed to fetch items data for version: {}", patchVersion, e);

            // 실패 시 최신 버전으로 재시도
            try {
                String latestVersion = dataDragonClient.getLatestVersion();
                log.info("Retrying with latest version: {}", latestVersion);
                return dataDragonClient.getItems(latestVersion);
            } catch (Exception ex) {
                log.error("Failed to fetch items data with latest version", ex);
                return null;
            }
        }
    }

    /**
     * HTML 태그 제거하여 순수 텍스트만 추출
     */
    private String extractPlainText(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * 게임 버전에서 패치 버전 추출
     * 예: "14.23.1.12345" -> "14.23"
     */
    private String extractPatchVersion(String gameVersion) {
        if (gameVersion == null || gameVersion.isEmpty()) {
            return getActiveVersion();
        }

        String[] parts = gameVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return gameVersion;
    }

    /**
     * 현재 활성화된 Data Dragon 버전 조회
     */
    public String getActiveVersion() {
        return versionRepository.findByIsActiveTrue()
                .map(DataDragonVersion::getVersion)
                .orElseGet(() -> {
                    log.warn("No active Data Dragon version found, fetching from API");
                    return dataDragonClient.getLatestVersion();
                });
    }

    /**
     * Data Dragon 버전 업데이트 (스케줄러에서 호출)
     */
    @Transactional
    public void updateDataDragonVersion() {
        try {
            String latestVersion = dataDragonClient.getLatestVersion();
            log.info("Fetched latest Data Dragon version: {}", latestVersion);

            // 기존 활성 버전 비활성화
            versionRepository.findByIsActiveTrue().ifPresent(DataDragonVersion::deactivate);

            // 새 버전이 이미 존재하는지 확인
            DataDragonVersion version = versionRepository.findByVersion(latestVersion)
                    .orElseGet(() -> DataDragonVersion.builder()
                            .version(latestVersion)
                            .isActive(false)
                            .build());

            // 활성화 및 저장
            version.activate();
            versionRepository.save(version);

            log.info("Data Dragon version updated to: {}", latestVersion);
        } catch (Exception e) {
            log.error("Failed to update Data Dragon version", e);
        }
    }

    /**
     * 초기 Data Dragon 버전 설정 (애플리케이션 시작 시 호출)
     */
    @Transactional
    public void initializeDataDragonVersion() {
        if (versionRepository.findByIsActiveTrue().isEmpty()) {
            log.info("No active Data Dragon version found, initializing...");
            updateDataDragonVersion();
        }
    }
}
