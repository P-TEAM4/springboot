package com.lol.highlight.domain.user.service;

import com.lol.highlight.domain.user.dto.UserSettingsResponse;
import com.lol.highlight.domain.user.dto.UserSettingsUpdateRequest;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.entity.UserSettings;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.domain.user.repository.UserSettingsRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettingsResponse::from)
                .orElse(UserSettingsResponse.defaults());
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 설정이 없으면 새로 생성
                    UserSettings newSettings = UserSettings.builder()
                            .user(user)
                            .autoLaunch(request.getAutoLaunch())
                            .autoShowOnLol(request.getAutoShowOnLol())
                            .build();
                    return userSettingsRepository.save(newSettings);
                });

        // 기존 설정 업데이트
        settings.updateSettings(request.getAutoLaunch(), request.getAutoShowOnLol());
        
        return UserSettingsResponse.from(settings);
    }
}
