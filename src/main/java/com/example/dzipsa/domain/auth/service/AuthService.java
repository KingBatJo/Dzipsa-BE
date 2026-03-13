package com.example.dzipsa.domain.auth.service;

import com.example.dzipsa.domain.auth.converter.AuthConverter;
import com.example.dzipsa.domain.auth.dto.request.RefreshTokenRequest;
import com.example.dzipsa.domain.auth.dto.response.TokenResponse;
import com.example.dzipsa.domain.auth.entity.RefreshToken;
import com.example.dzipsa.domain.auth.repository.OAuthAccountRepository;
import com.example.dzipsa.domain.auth.repository.RefreshTokenRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.entity.enums.UserRole;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.AuthErrorCode;
import com.example.dzipsa.global.exception.domain.UserErrorCode;
import com.example.dzipsa.global.redis.RedisUtil;
import com.example.dzipsa.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_VALID_DAYS = 14;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthConverter authConverter;
    private final RedisUtil redisUtil;

    @Transactional
    public TokenResponse issueTokenResponse(Long userId, String email, UserRole role) {
        log.debug("[AuthService] issueTokenResponse userId={}", userId);
        String accessToken = jwtTokenProvider.createAccessToken(userId, email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, email, role);

        refreshTokenRepository.deleteByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .createdAt(now)
                .expiredAt(now.plusDays(REFRESH_TOKEN_VALID_DAYS))
                .build());

        Long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationMs();
        return authConverter.toTokenResponse(accessToken, refreshToken, accessTokenExpiresIn);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request, String oldAccessToken) {
        log.info("[AuthService] refresh 요청");
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            log.warn("[AuthService] refresh 실패 - 토큰 유효성 실패");
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("[AuthService] refresh 실패 - 저장된 토큰 없음");
                    return new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
                });

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.delete(storedToken);

        // 기존 Access Token을 Blacklist에 추가 (유효한 경우에만)
        if (StringUtils.hasText(oldAccessToken) && jwtTokenProvider.validateAccessToken(oldAccessToken)) {
            Date expiration = jwtTokenProvider.getExpirationDateFromAccessToken(oldAccessToken);
            long expirationTime = (expiration.getTime() - System.currentTimeMillis()) / 1000 / 60;
            if (expirationTime > 0) {
                redisUtil.setBlackList(oldAccessToken, "refresh", expirationTime);
                log.info("[AuthService] 기존 Access Token 블랙리스트 처리 완료");
            }
        }

        return issueTokenResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        log.info("[AuthService] logout 요청");
        
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }

        // Access Token Blacklist 처리
        if (StringUtils.hasText(accessToken) && jwtTokenProvider.validateAccessToken(accessToken)) {
            Date expiration = jwtTokenProvider.getExpirationDateFromAccessToken(accessToken);
            long expirationTime = (expiration.getTime() - System.currentTimeMillis()) / 1000 / 60;
            if (expirationTime > 0) {
                redisUtil.setBlackList(accessToken, "logout", expirationTime);
                log.info("[AuthService] Access Token 블랙리스트 처리 완료 (logout)");
            }
        }
    }

    @Transactional
    public void withdraw(Long userId, String accessToken) {
        log.info("[AuthService] withdraw 요청 userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 1. 소셜 연동 정보 삭제 (재가입을 위해)
        oAuthAccountRepository.findByUserId(userId).ifPresent(oAuthAccountRepository::delete);

        // 2. 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 3. 유저 상태 변경 (논리적 삭제)
        user.withdraw();

        // 4. Access Token Blacklist 처리
        if (StringUtils.hasText(accessToken) && jwtTokenProvider.validateAccessToken(accessToken)) {
            Date expiration = jwtTokenProvider.getExpirationDateFromAccessToken(accessToken);
            long expirationTime = (expiration.getTime() - System.currentTimeMillis()) / 1000 / 60;
            if (expirationTime > 0) {
                redisUtil.setBlackList(accessToken, "withdraw", expirationTime);
                log.info("[AuthService] Access Token 블랙리스트 처리 완료 (withdraw)");
            }
        }
        log.info("[AuthService] 회원 탈퇴 처리 완료. userId={}", userId);
    }
}
