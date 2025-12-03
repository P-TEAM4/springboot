package com.lol.highlight.domain.session.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

    private String refreshToken;
    private String deviceId;
}
