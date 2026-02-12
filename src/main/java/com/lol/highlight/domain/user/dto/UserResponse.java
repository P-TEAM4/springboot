package com.lol.highlight.domain.user.dto;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.auth.enums.AuthProvider;
import com.lol.highlight.domain.user.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImage;
    private String riotId;
    private String puuid;
    private String summonerName;
    private String tagLine;
    private Integer profileIconId;
    private Long summonerLevel;
    private String tier;
    private String rank;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private Double winRate;
    private Double averageKda;
    private Double averageVisionScore;
    private Double averageCsPerMin;
    private AuthProvider provider;
    private String providerId;
    private UserRole role;
    private LocalDateTime lastActivityAt;
    private LocalDateTime lastMatchRefreshAt;
    private Integer refreshCountInWindow;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .riotId(user.getRiotId())
                .puuid(user.getPuuid())
                .summonerName(user.getSummonerName())
                .tagLine(user.getTagLine())
                .profileIconId(user.getProfileIconId())
                .summonerLevel(user.getSummonerLevel())
                .tier(user.getTier())
                .rank(user.getRank())
                .leaguePoints(user.getLeaguePoints())
                .wins(user.getWins())
                .losses(user.getLosses())
                .winRate(user.getWinRate())
                .averageKda(user.getAverageKda())
                .averageVisionScore(user.getAverageVisionScore())
                .averageCsPerMin(user.getAverageCsPerMin())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .lastActivityAt(user.getLastActivityAt())
                .lastMatchRefreshAt(user.getLastMatchRefreshAt())
                .refreshCountInWindow(user.getRefreshCountInWindow())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
