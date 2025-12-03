package com.lol.highlight.global.auth.oauth2;

import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.enums.AuthProvider;
import com.lol.highlight.domain.user.enums.UserRole;
import com.lol.highlight.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        log.debug("OAuth2 Login - Provider: {}, Attributes: {}", registrationId, attributes);

        User user = processOAuth2User(registrationId, attributes);

        return oAuth2User;
    }

    private User processOAuth2User(String registrationId, Map<String, Object> attributes) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        return userRepository.findByEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, name, picture))
                .orElseGet(() -> createNewUser(email, name, picture, provider, providerId));
    }

    private User updateExistingUser(User user, String name, String profileImage) {
        user.updateProfile(name, profileImage);
        return userRepository.save(user);
    }

    private User createNewUser(String email, String name, String profileImage,
                              AuthProvider provider, String providerId) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .profileImage(profileImage)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .build();

        return userRepository.save(newUser);
    }
}
