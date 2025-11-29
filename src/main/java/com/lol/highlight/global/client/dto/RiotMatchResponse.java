package com.lol.highlight.global.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiotMatchResponse {
    private String matchId;
    private Long gameCreation;
    private Long gameDuration;
    private String gameMode;
    private String gameType;
    private List<Participant> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant {
        private String puuid;
        private String summonerName;
        private String championName;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Boolean win;
        private String lane;
        private String role;
        private Integer totalMinionsKilled;
        private Integer visionScore;
    }
}
