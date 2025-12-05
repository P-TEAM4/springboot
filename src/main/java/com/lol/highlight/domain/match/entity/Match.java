package com.lol.highlight.domain.match.entity;

import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String matchId;

    @Column(nullable = false)
    private String championName;

    private Integer kills;

    private Integer deaths;

    private Integer assists;

    private Double kda;

    private Boolean win;

    private Integer gameDuration;

    private Long gameCreation;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Column(columnDefinition = "TEXT")
    private String timelineData;

    private String detailDataUrl;

    @Builder
    public Match(User user, String matchId, String championName, Integer kills,
                Integer deaths, Integer assists, Double kda, Boolean win,
                Integer gameDuration, Long gameCreation, MatchStatus status,
                String timelineData, String detailDataUrl) {
        this.user = user;
        this.matchId = matchId;
        this.championName = championName;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.kda = kda;
        this.win = win;
        this.gameDuration = gameDuration;
        this.gameCreation = gameCreation;
        this.status = status != null ? status : MatchStatus.PENDING;
        this.timelineData = timelineData;
        this.detailDataUrl = detailDataUrl;
    }

    public void updateMatchData(String championName, Integer kills, Integer deaths,
                               Integer assists, Double kda, Boolean win,
                               Integer gameDuration, Long gameCreation,
                               String timelineData, String detailDataUrl) {
        this.championName = championName;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.kda = kda;
        this.win = win;
        this.gameDuration = gameDuration;
        this.gameCreation = gameCreation;
        this.timelineData = timelineData;
        this.detailDataUrl = detailDataUrl;
        this.status = MatchStatus.COMPLETED;
    }

    public void updateDetailDataUrl(String detailDataUrl) {
        this.detailDataUrl = detailDataUrl;
    }

    public void updateStatus(MatchStatus status) {
        this.status = status;
    }
}
