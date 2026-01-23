package com.lol.highlight.domain.user.entity;

import com.lol.highlight.global.auth.enums.AuthProvider;
import com.lol.highlight.domain.user.enums.UserRole;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    @Column(unique = true)
    private String riotId;

    private String summonerName;

    private String tagLine;

    private Integer profileIconId;

    private Long summonerLevel;

    private String tier;

    @Column(name = "league_rank")
    private String rank;

    private Integer leaguePoints;

    private Integer wins;

    private Integer losses;

    private Double winRate;

    private Double averageKda;

    private Double averageVisionScore;

    private Double averageCsPerMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private LocalDateTime lastActivityAt;

    private LocalDateTime lastMatchRefreshAt;

    @Column(nullable = false)
    private Integer refreshCountInWindow = 0;

    @Builder
    public User(String email, String name, String profileImage, String riotId,
                String summonerName, String tagLine, AuthProvider provider,
                String providerId, UserRole role) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.riotId = riotId;
        this.summonerName = summonerName;
        this.tagLine = tagLine;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role != null ? role : UserRole.USER;
    }

    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    public void linkRiotAccount(String riotId, String summonerName, String tagLine) {
        this.riotId = riotId;
        this.summonerName = summonerName;
        this.tagLine = tagLine;
    }

    public void unlinkRiotAccount() {
        this.riotId = null;
        this.summonerName = null;
        this.tagLine = null;
        this.profileIconId = null;
        this.summonerLevel = null;
        this.tier = null;
        this.rank = null;
        this.leaguePoints = null;
        this.wins = null;
        this.losses = null;
        this.winRate = null;
        this.averageKda = null;
        this.averageVisionScore = null;
        this.averageCsPerMin = null;
    }

    public void updateLastActivityAt() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void updateLastMatchRefreshAt() {
        this.lastMatchRefreshAt = LocalDateTime.now();
    }

    public boolean canRefreshMatches(int maxRefreshCount, int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();

        if (lastMatchRefreshAt == null) {
            return true;
        }

        // 윈도우가 지났으면 리셋
        if (now.isAfter(lastMatchRefreshAt.plusMinutes(windowMinutes))) {
            return true;
        }

        // 윈도우 내: 횟수 체크
        return refreshCountInWindow < maxRefreshCount;
    }

    public void recordRefresh(int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();

        // 새 윈도우 시작
        if (lastMatchRefreshAt == null ||
            now.isAfter(lastMatchRefreshAt.plusMinutes(windowMinutes))) {
            this.refreshCountInWindow = 1;
            this.lastMatchRefreshAt = now;
        } else {
            // 같은 윈도우 내: 카운트 증가
            this.refreshCountInWindow++;
        }
    }

    public void updateSummonerInfo(Integer profileIconId, Long summonerLevel,
                                   String tier, String rank, Integer leaguePoints,
                                   Integer wins, Integer losses) {
        this.profileIconId = profileIconId;
        this.summonerLevel = summonerLevel;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
        this.wins = wins;
        this.losses = losses;
        if (wins != null && losses != null && (wins + losses) > 0) {
            this.winRate = (double) wins / (wins + losses) * 100;
        }
    }

    public void updateStatistics(Double averageKda, Double averageVisionScore, Double averageCsPerMin) {
        this.averageKda = averageKda;
        this.averageVisionScore = averageVisionScore;
        this.averageCsPerMin = averageCsPerMin;
    }
}
