package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChampionStatsResponse {

    private List<ChampionStat> champions;
    private long totalMatches;

    @Getter
    @Builder
    public static class ChampionStat {
        private String championName;
        private long totalGames;
        private double winRate;
        private double pickRate;
        private double banRate;
        private double avgKills;
        private double avgDeaths;
        private double avgAssists;
        private String tier;
    }
}
