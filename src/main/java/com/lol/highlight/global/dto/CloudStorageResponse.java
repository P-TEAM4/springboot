package com.lol.highlight.global.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CloudStorageResponse {

    private String matchId;
    private String gameVersion;
    private List<PlayerData> players;
    private List<TeamData> teams;

    @Getter
    @Builder
    public static class PlayerData {
        private String playerName;
        private String championName;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Integer totalDamageDealt;
        private Integer visionScore;
        private Integer cs;
        private List<Integer> finalItems;
        private Integer goldEarned;
        private List<ItemBuildData> itemBuild;
        private List<Integer> skillBuild;
    }

    @Getter
    @Builder
    public static class ItemBuildData {
        private Integer itemId;
        private Integer timestamp;
    }

    @Getter
    @Builder
    public static class TeamData {
        private Integer teamId;
        private Boolean win;
        private Integer totalObjectives;
        private Integer totalKills;
    }
}
