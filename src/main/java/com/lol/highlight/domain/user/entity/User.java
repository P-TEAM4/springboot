package com.lol.highlight.domain.user.entity;

import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    // 기본 정보
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    // Riot 계정 정보
    @Column(unique = true)
    private String riotPuuid;  // Riot PUUID (고유 식별자)

    private String summonerName;  // 소환사명 (예: Hide on bush)

    private String tagLine;  // 태그라인 (예: KR1)

    // OAuth2 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;  // OAuth2 제공자 (GOOGLE, KAKAO 등)

    private String providerId;  // OAuth2 제공자의 고유 ID

    // 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder
    public User(String email, String name, String profileImage,
                String riotPuuid, String summonerName, String tagLine,
                AuthProvider provider, String providerId, UserRole role) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.riotPuuid = riotPuuid;
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

    public void linkRiotAccount(String riotPuuid, String summonerName, String tagLine) {
        this.riotPuuid = riotPuuid;
        this.summonerName = summonerName;
        this.tagLine = tagLine;
    }

    // Riot ID 포맷: summonerName#tagLine
    public String getRiotId() {
        if (summonerName != null && tagLine != null) {
            return summonerName + "#" + tagLine;
        }
        return null;
    }
}
