package com.lol.highlight.domain.user.dto;

import com.lol.highlight.domain.user.entity.UserSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSettingsResponse {
    
    private Boolean autoLaunch;
    private Boolean autoShowOnLol;
    
    public static UserSettingsResponse from(UserSettings settings) {
        return UserSettingsResponse.builder()
                .autoLaunch(settings.getAutoLaunch())
                .autoShowOnLol(settings.getAutoShowOnLol())
                .build();
    }
    
    // 기본값 반환 (설정이 없을 경우)
    public static UserSettingsResponse defaults() {
        return UserSettingsResponse.builder()
                .autoLaunch(false)
                .autoShowOnLol(true)
                .build();
    }
}
