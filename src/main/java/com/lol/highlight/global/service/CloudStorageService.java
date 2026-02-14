package com.lol.highlight.global.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.global.dto.CloudStorageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageService {

    private final Storage storage;
    private final ObjectMapper objectMapper;

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    @Value("${gcp.storage.match-data-prefix:match-data}")
    private String matchDataPrefix;

    @Value("${gcp.storage.profile-image-prefix:profile-images}")
    private String profileImagePrefix;

    /**
     * 프로필 이미지 업로드
     * @param userId 사용자 ID
     * @param imageBytes 이미지 바이트 데이터
     * @param contentType 이미지 타입 (image/jpeg, image/png 등)
     * @return 업로드된 이미지 URL
     */
    public String uploadProfileImage(Long userId, byte[] imageBytes, String contentType) {
        try {
            // 파일 확장자 추출
            String extension = getExtensionFromContentType(contentType);
            String objectName = String.format("%s/%s%s", profileImagePrefix, userId, extension);

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            storage.create(blobInfo, imageBytes);

            String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
            log.info("Successfully uploaded profile image to Cloud Storage: {}", url);

            return url;

        } catch (Exception e) {
            log.error("Failed to upload profile image to Cloud Storage for userId: {}", userId, e);
            throw new RuntimeException("Failed to upload profile image to Cloud Storage", e);
        }
    }

    /**
     * Content-Type에서 파일 확장자 추출
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }
        
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public String uploadMatchData(String matchId, MatchDetailResponse matchDetail) {
        try {
            // 게임 버전 추출 (예: 14.23.1.12345 -> 14.23)
            String gameVersion = extractPatchVersion(matchDetail.getGameVersion());
            String objectName = String.format("%s/%s/%s.csv", matchDataPrefix, gameVersion, matchId);
            String csvData = convertToCsv(matchDetail);

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/csv")
                    .build();

            storage.create(blobInfo, csvData.getBytes(StandardCharsets.UTF_8));

            String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
            log.info("Successfully uploaded match data to Cloud Storage: {}", url);

            return url;

        } catch (Exception e) {
            log.error("Failed to upload match data to Cloud Storage for matchId: {}", matchId, e);
            throw new RuntimeException("Failed to upload match data to Cloud Storage", e);
        }
    }

    public CloudStorageResponse downloadMatchData(String detailDataUrl) {
        try {
            String objectName = extractObjectNameFromUrl(detailDataUrl);
            BlobId blobId = BlobId.of(bucketName, objectName);

            byte[] data = storage.readAllBytes(blobId);
            String csvData = new String(data, StandardCharsets.UTF_8);

            CloudStorageResponse storageResponse = parseCsv(csvData);
            log.info("Successfully downloaded match data from Cloud Storage: {}", detailDataUrl);

            return storageResponse;

        } catch (Exception e) {
            log.error("Failed to download match data from Cloud Storage: {}", detailDataUrl, e);
            throw new RuntimeException("Failed to download match data from Cloud Storage", e);
        }
    }

    private String extractObjectNameFromUrl(String url) {
        int bucketEndIndex = url.indexOf(bucketName + "/");
        if (bucketEndIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloud Storage URL format: " + url);
        }
        return url.substring(bucketEndIndex + bucketName.length() + 1);
    }

    private String convertToCsv(MatchDetailResponse matchDetail) {
        StringBuilder csv = new StringBuilder();

        csv.append("playerName,championName,kills,deaths,assists,totalDamageDealt,visionScore,cs,finalItems,goldEarned\n");

        for (MatchDetailResponse.PlayerDetail player : matchDetail.getPlayers()) {
            csv.append(escapeCSV(player.getPlayerName())).append(",");
            csv.append(escapeCSV(player.getChampionName())).append(",");
            csv.append(player.getKills()).append(",");
            csv.append(player.getDeaths()).append(",");
            csv.append(player.getAssists()).append(",");
            csv.append(player.getTotalDamageDealt()).append(",");
            csv.append(player.getVisionScore()).append(",");
            csv.append(player.getCs()).append(",");
            csv.append(escapeCSV(player.getFinalItems().toString())).append(",");
            csv.append(player.getGoldEarned()).append("\n");
        }

        csv.append("\nTEAMS\n");
        csv.append("teamId,win,totalObjectives,totalKills\n");

        for (MatchDetailResponse.TeamDetail team : matchDetail.getTeams()) {
            csv.append(team.getTeamId()).append(",");
            csv.append(team.getWin()).append(",");
            csv.append(team.getTotalObjectives()).append(",");
            csv.append(team.getTotalKills()).append("\n");
        }

        return csv.toString();
    }

    private CloudStorageResponse parseCsv(String csvData) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(csvData));
        List<CloudStorageResponse.PlayerData> players = new ArrayList<>();
        List<CloudStorageResponse.TeamData> teams = new ArrayList<>();

        String line;
        boolean isTeamSection = false;
        boolean isHeader = true;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            if (line.equals("TEAMS")) {
                isTeamSection = true;
                isHeader = true;
                continue;
            }

            if (isHeader) {
                isHeader = false;
                continue;
            }

            if (!isTeamSection) {
                int arrayStart = line.indexOf('[');
                int arrayEnd = line.indexOf(']');

                String beforeArray = line.substring(0, arrayStart);
                String arrayContent = line.substring(arrayStart, arrayEnd + 1);
                String afterArray = line.substring(arrayEnd + 1);

                String[] beforeValues = beforeArray.split(",");
                String[] afterValues = afterArray.split(",");

                CloudStorageResponse.PlayerData player = CloudStorageResponse.PlayerData.builder()
                        .playerName(beforeValues[0].trim())
                        .championName(beforeValues[1].trim())
                        .kills(Integer.parseInt(beforeValues[2].trim()))
                        .deaths(Integer.parseInt(beforeValues[3].trim()))
                        .assists(Integer.parseInt(beforeValues[4].trim()))
                        .totalDamageDealt(Integer.parseInt(beforeValues[5].trim()))
                        .visionScore(Integer.parseInt(beforeValues[6].trim()))
                        .cs(Integer.parseInt(beforeValues[7].trim()))
                        .finalItems(parseIntegerList(arrayContent))
                        .goldEarned(Integer.parseInt(afterValues[1].trim()))
                        .itemBuild(new ArrayList<>())
                        .skillBuild(new ArrayList<>())
                        .build();
                players.add(player);
            } else {
                String[] values = line.split(",");
                CloudStorageResponse.TeamData team = CloudStorageResponse.TeamData.builder()
                        .teamId(Integer.parseInt(values[0].trim()))
                        .win(Boolean.parseBoolean(values[1].trim()))
                        .totalObjectives(Integer.parseInt(values[2].trim()))
                        .totalKills(Integer.parseInt(values[3].trim()))
                        .build();
                teams.add(team);
            }
        }

        return CloudStorageResponse.builder()
                .matchId("")
                .gameVersion(null)
                .players(players)
                .teams(teams)
                .build();
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<Integer> parseIntegerList(String value) {
        value = value.replace("[", "").replace("]", "").trim();
        if (value.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = value.split(",\\s*");
        List<Integer> result = new ArrayList<>();
        for (String part : parts) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                result.add(0);
            }
        }
        return result;
    }

    /**
     * 게임 버전에서 패치 버전 추출
     * 예: "14.23.1.12345" -> "14.23"
     */
    private String extractPatchVersion(String gameVersion) {
        if (gameVersion == null || gameVersion.isEmpty()) {
            return "unknown";
        }

        String[] parts = gameVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return gameVersion;
    }
}
