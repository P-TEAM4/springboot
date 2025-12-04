package com.lol.highlight.global.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.match-data-prefix:match-data}")
    private String matchDataPrefix;

    public String uploadMatchData(String matchId, MatchDetailResponse matchDetail) {
        try {
            String key = String.format("%s/%s.csv", matchDataPrefix, matchId);
            String csvData = convertToCsv(matchDetail);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(csvData, StandardCharsets.UTF_8));

            String url = String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
            log.info("Successfully uploaded match data to S3: {}", url);

            return url;

        } catch (Exception e) {
            log.error("Failed to upload match data to S3 for matchId: {}", matchId, e);
            throw new RuntimeException("Failed to upload match data to S3", e);
        }
    }

    public MatchDetailResponse downloadMatchData(String detailDataUrl) {
        try {
            String key = extractKeyFromUrl(detailDataUrl);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            byte[] data = s3Client.getObject(getObjectRequest).readAllBytes();
            String csvData = new String(data, StandardCharsets.UTF_8);

            MatchDetailResponse matchDetail = parseCsv(csvData);
            log.info("Successfully downloaded match data from S3: {}", detailDataUrl);

            return matchDetail;

        } catch (IOException e) {
            log.error("Failed to download match data from S3: {}", detailDataUrl, e);
            throw new RuntimeException("Failed to download match data from S3", e);
        }
    }

    private String extractKeyFromUrl(String url) {
        int bucketEndIndex = url.indexOf(".s3.amazonaws.com/");
        if (bucketEndIndex == -1) {
            throw new IllegalArgumentException("Invalid S3 URL format: " + url);
        }
        return url.substring(bucketEndIndex + ".s3.amazonaws.com/".length());
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

    private MatchDetailResponse parseCsv(String csvData) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(csvData));
        List<MatchDetailResponse.PlayerDetail> players = new ArrayList<>();
        List<MatchDetailResponse.TeamDetail> teams = new ArrayList<>();

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

            String[] values = line.split(",");

            if (!isTeamSection) {
                MatchDetailResponse.PlayerDetail player = MatchDetailResponse.PlayerDetail.builder()
                        .playerName(values[0])
                        .championName(values[1])
                        .kills(Integer.parseInt(values[2]))
                        .deaths(Integer.parseInt(values[3]))
                        .assists(Integer.parseInt(values[4]))
                        .totalDamageDealt(Integer.parseInt(values[5]))
                        .visionScore(Integer.parseInt(values[6]))
                        .cs(Integer.parseInt(values[7]))
                        .finalItems(parseIntegerList(values[8]))
                        .goldEarned(Integer.parseInt(values[9]))
                        .itemBuild(new ArrayList<>())
                        .skillBuild(new ArrayList<>())
                        .build();
                players.add(player);
            } else {
                MatchDetailResponse.TeamDetail team = MatchDetailResponse.TeamDetail.builder()
                        .teamId(Integer.parseInt(values[0]))
                        .win(Boolean.parseBoolean(values[1]))
                        .totalObjectives(Integer.parseInt(values[2]))
                        .totalKills(Integer.parseInt(values[3]))
                        .build();
                teams.add(team);
            }
        }

        return MatchDetailResponse.builder()
                .matchId("")
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
}
