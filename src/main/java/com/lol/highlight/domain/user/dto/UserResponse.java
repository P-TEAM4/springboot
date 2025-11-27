package com.lol.highlight.domain.user.dto;

import com.lol.highlight.domain.user.entity.AuthProvider;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.entity.UserRole;
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
    private String summonerName;
    private String tagLine;
    private AuthProvider provider;
    private UserRole role;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .riotId(user.getRiotId())
                .summonerName(user.getSummonerName())
                .tagLine(user.getTagLine())
                .provider(user.getProvider())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
