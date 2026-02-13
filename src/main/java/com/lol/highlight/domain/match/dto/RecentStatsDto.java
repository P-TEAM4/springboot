package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecentStatsDto {
    private Integer totalGames;    // 최근 경기 수
    private Integer wins;           // 승
    private Integer losses;         // 패
    private String winRate;         // 승률 (%)
    private Double averageKda;      // 평균 KDA
}
