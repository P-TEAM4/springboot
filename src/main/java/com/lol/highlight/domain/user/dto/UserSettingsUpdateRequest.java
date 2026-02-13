package com.lol.highlight.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSettingsUpdateRequest {
    
    private Boolean autoLaunch;
    private Boolean autoShowOnLol;
}
