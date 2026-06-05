package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchDetailResponse {

    private String matchId;
    private String gameVersion;
    private List<PlayerDetail> players;
    private List<TeamDetail> teams;

    @Getter
    @Builder
    public static class PlayerDetail {
        private String playerName;
        private String championName;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Integer totalDamageDealt;
        private Integer visionScore;
        private Integer cs;
        private List<Integer> finalItems;
        private List<ItemInfo> finalItemsInfo;
        private Integer goldEarned;
        private List<ItemBuild> itemBuild;
        private List<Integer> skillBuild;
    }

    @Getter
    @Builder
    public static class ItemBuild {
        private Integer itemId;
        private Long timestamp;
    }

    @Getter
    @Builder
    public static class TeamDetail {
        private Integer teamId;
        private Boolean win;
        private Integer totalObjectives;
        private Integer totalKills;
    }

    @Getter
    @Builder
    public static class ItemInfo {
        private Integer itemId;
        private String itemName;
        private String itemDescription;
        private String itemImageUrl;
    }
}
