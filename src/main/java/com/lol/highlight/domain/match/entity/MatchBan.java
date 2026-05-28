package com.lol.highlight.domain.match.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_bans", indexes = {
        @Index(name = "idx_match_ban_champion", columnList = "championName"),
        @Index(name = "idx_match_ban_match_id", columnList = "matchId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String matchId;

    @Column(nullable = false)
    private String championName;

    @Builder
    public MatchBan(String matchId, String championName) {
        this.matchId = matchId;
        this.championName = championName;
    }
}
