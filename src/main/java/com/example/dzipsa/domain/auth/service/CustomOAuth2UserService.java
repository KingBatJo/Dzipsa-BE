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
import java.util.Optional;
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
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("[CustomOAuth2UserService] loadUser registrationId={}", registrationId);

        OAuth2User oAuth2User = super.loadUser(userRequest);
        OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

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

        // 1. provider와 providerId로 OAuthAccount를 찾는다. (가장 정확한 식별 방법)
        Optional<OAuthAccount> oauthAccountOpt = oauthAccountRepository.findByProviderAndProviderId(provider, userInfo.getProviderId());

        if (oauthAccountOpt.isPresent()) {
            // 1-1. OAuthAccount가 존재하면, 기존 유저이므로 로그인 처리
            log.debug("[CustomOAuth2UserService] 기존 OAuth 계정 로그인 userId={}", oauthAccountOpt.get().getUser().getId());
            return oauthAccountOpt.get().getUser();
        } else {
            // 1-2. OAuthAccount가 없으면, 신규 가입 또는 재가입
            return handleNewOrRejoiningUser(userInfo, provider, isNewUser);
        }
    }

    private User handleNewOrRejoiningUser(OAuth2UserInfo userInfo, OAuthProvider provider, AtomicBoolean isNewUser) {
        ProviderType providerType = ProviderType.valueOf(userInfo.getProvider().toUpperCase());
        
        // 2. 이메일과 ProviderType으로 기존 유저가 있는지 확인한다.
        Optional<User> userOpt = userRepository.findByEmailAndProviderType(userInfo.getEmail(), providerType);

        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            // 2-1. 이메일과 Provider가 모두 일치하고, 상태가 DELETED인 경우 -> 재가입 처리
            if (existingUser.getStatus() == UserStatus.DELETED) {
                log.info("[CustomOAuth2UserService] 탈퇴한 사용자 재가입 처리. userId={}", existingUser.getId());
                // 닉네임이 null인 경우 "임시닉네임" 문자열로 대체
                String nickname = userInfo.getNickname() != null ? userInfo.getNickname() : "임시닉네임";
                existingUser.rejoin(nickname);
                createOAuthAccount(existingUser, provider, userInfo.getProviderId());
                return existingUser;
            }
            // 2-2. 그 외의 경우 (논리적으로는 발생하기 어려움. OAuthAccount가 없는데 User는 있는 경우)
            // 혹시 모를 데이터 불일치 상황에 대비해, 기존 계정에 OAuth 정보만 추가해준다.
            log.warn("[CustomOAuth2UserService] 데이터 불일치 의심: User는 존재하나 OAuthAccount가 없음. userId={}", existingUser.getId());
            createOAuthAccount(existingUser, provider, userInfo.getProviderId());
            return existingUser;
        } else {
            // 3. 완전히 새로운 유저인 경우 (이메일이 같더라도 Provider가 다르면 여기에 해당) -> 신규 가입 처리
            isNewUser.set(true);
            return createUserAndOAuthAccount(userInfo, provider);
        }
    }

    private User createUserAndOAuthAccount(OAuth2UserInfo userInfo, OAuthProvider provider) {
        log.info("[CustomOAuth2UserService] 신규 사용자 가입 처리. email={}, provider={}", userInfo.getEmail(), provider.name());
        
        // 닉네임이 null인 경우 "임시닉네임" 문자열로 대체
        String nickname = userInfo.getNickname() != null ? userInfo.getNickname() : "임시닉네임";
        
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.valueOf(userInfo.getProvider().toUpperCase()))
                .build();
        userRepository.save(newUser);

        createOAuthAccount(newUser, provider, userInfo.getProviderId());

        return newUser;
    }

    private void createOAuthAccount(User user, OAuthProvider provider, String providerId) {
        OAuthAccount newAccount = OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .createdAt(LocalDateTime.now())
                .build();
        oauthAccountRepository.save(newAccount);
        log.info("[CustomOAuth2UserService] OAuth 계정 생성/연결 완료. userId={}, provider={}", user.getId(), provider);
    }
}
