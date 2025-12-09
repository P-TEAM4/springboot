package com.lol.highlight.global.external.datadragon;

import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataDragonService {

    private final DataDragonClient dataDragonClient;

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
            return dataDragonClient.getLatestVersion();
        }

        String[] parts = gameVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return gameVersion;
    }
}
