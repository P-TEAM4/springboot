package com.lol.highlight.domain.match.entity;

import com.lol.highlight.domain.match.enums.MatchStatus;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_puuid_game_creation", columnList = "puuid, gameCreation")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @Column(nullable = false)
    private String puuid;

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

    // 아이템 정보
    private Integer item0;
    private Integer item1;
    private Integer item2;
    private Integer item3;
    private Integer item4;
    private Integer item5;
    private Integer item6;

    // 게임 버전 (패치 버전)
    private String gameVersion;

    @Builder
    public Match(String puuid, String matchId, String championName, Integer kills,
                Integer deaths, Integer assists, Double kda, Boolean win,
                Integer gameDuration, Long gameCreation, MatchStatus status,
                String timelineData, String detailDataUrl,
                Integer item0, Integer item1, Integer item2, Integer item3,
                Integer item4, Integer item5, Integer item6, String gameVersion) {
        this.puuid = puuid;
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
        this.item0 = item0;
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.item5 = item5;
        this.item6 = item6;
        this.gameVersion = gameVersion;
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
