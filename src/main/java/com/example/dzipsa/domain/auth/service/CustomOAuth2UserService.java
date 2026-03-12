package com.example.dzipsa.domain.auth.service;

import com.example.dzipsa.domain.auth.dto.info.KakaoOAuth2UserInfo;
import com.example.dzipsa.domain.auth.dto.info.NaverOAuth2UserInfo;
import com.example.dzipsa.domain.auth.dto.info.OAuth2UserInfo;
import com.example.dzipsa.domain.auth.entity.OAuthAccount;
import com.example.dzipsa.domain.auth.entity.enums.OAuthProvider;
import com.example.dzipsa.domain.auth.entity.enums.ProviderType;
import com.example.dzipsa.domain.auth.repository.OAuthAccountRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.entity.enums.UserStatus;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.security.CustomPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 확인 (naver, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("[CustomOAuth2UserService] loadUser registrationId={}", registrationId);

        OAuth2User oAuth2User = super.loadUser(userRequest);
        OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 신규 가입 여부를 확인하기 위한 변수
        AtomicBoolean isNewUser = new AtomicBoolean(false);

        User user = saveOrUpdate(oAuth2UserInfo, isNewUser);

        log.info("[CustomOAuth2UserService] loadUser 완료 userId={}, provider={}, isNewUser={}", 
                user.getId(), registrationId, isNewUser.get());

        return new CustomPrincipal(user, oAuth2User.getAttributes(), isNewUser.get());
    }

    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다. ID: " + registrationId);
        };
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo, AtomicBoolean isNewUser) {
        OAuthProvider provider = OAuthProvider.valueOf(userInfo.getProvider().toUpperCase());

        return oauthAccountRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(OAuthAccount::getUser)
                .map(user -> {
                    log.debug("[CustomOAuth2UserService] 기존 OAuth 계정 로그인 userId={}", user.getId());
                    return user;
                })
                .orElseGet(() -> {
                    isNewUser.set(true); // 신규 생성 시 true 설정
                    return createUserAndOAuthAccount(userInfo, provider);
                });
    }

    private User createUserAndOAuthAccount(OAuth2UserInfo userInfo, OAuthProvider provider) {
        String email = userInfo.getEmail();

        String nickname = userInfo.getNickname();

        User newUser = User.builder()
                .email(email)
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.valueOf(userInfo.getProvider().toUpperCase()))
                .build();
        userRepository.save(newUser);

        OAuthAccount newAccount = OAuthAccount.builder()
                .user(newUser)
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .createdAt(LocalDateTime.now())
                .build();
        oauthAccountRepository.save(newAccount);
        log.info("[CustomOAuth2UserService] 신규 OAuth 가입 완료 userId={}, provider={}", newUser.getId(), provider);

        return newUser;
    }
}
