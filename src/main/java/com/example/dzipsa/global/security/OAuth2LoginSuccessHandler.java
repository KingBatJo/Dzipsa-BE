package com.example.dzipsa.global.security;

import com.example.dzipsa.domain.auth.dto.response.TokenResponse;
import com.example.dzipsa.domain.auth.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        // CustomPrincipal에서 필요한 정보 추출하여 토큰 발급 요청
        TokenResponse tokenResponse = authService.issueTokenResponse(
                principal.getUserId(), 
                principal.getEmail(), 
                principal.getRole()
        );

        // Refresh Token 쿠키 설정 (httpOnly=true, Secure=true, SameSite=None)
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(14 * 24 * 60 * 60) // 14일
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 신규 가입 여부, Access Token 파라미터 추가
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("created", principal.isNewUser())
                .build()
                .toUriString();

        log.info("[OAuth2LoginSuccessHandler] 로그인 성공, created={}, 리다이렉트: {}", principal.isNewUser(), targetUrl);
        
        if (response.isCommitted()) {
            log.warn("[OAuth2LoginSuccessHandler] 응답이 이미 커밋되어 리다이렉트 할 수 없습니다.");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
