package com.lol.highlight.domain.match.entity;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매치 엔티티
 * - 모든 조회된 매치를 영구 저장 (TTL 없음)
 * - 필요한 최소한의 정보만 저장 (timelineData 제외)
 * - timelineData는 분석 시에만 별도로 조회/저장
 */
@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_match_id", columnList = "matchId"),
        @Index(name = "idx_user_game_creation", columnList = "user_id, gameCreation")
})
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

    @Builder
    public Match(User user, String matchId, String championName, Integer kills,
                Integer deaths, Integer assists, Double kda, Boolean win,
                Integer gameDuration, Long gameCreation, MatchStatus status) {
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
    }

    public void updateStatus(MatchStatus status) {
        this.status = status;
    }
}
