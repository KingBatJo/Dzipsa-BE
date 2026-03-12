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
        TokenResponse tokenResponse = authService.issueTokenResponse(principal.getUser());


        // Refresh Token 쿠키 설정 (httpOnly=true, Secure=true, SameSite=None)
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 신규 가입 여부, Access Token 파라미터 추가
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("created", principal.isNewUser())
                .build()
                .toUriString();

        log.info("[OAuth2LoginSuccessHandler] 로그인 성공, created={}, 리다이렉트: {}", principal.isNewUser(), targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
