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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    @Column(unique = true)
    private String riotId;

    private String summonerName;

    private String tagLine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

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
}
