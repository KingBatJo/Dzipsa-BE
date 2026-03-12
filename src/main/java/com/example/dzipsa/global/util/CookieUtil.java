package com.example.dzipsa.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${app.cookie.secure}")
    private boolean secure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

    // Refresh Token 쿠키 생성 (HttpOnly=true)
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite) // 환경 변수에 따라 Lax/None 적용
                .maxAge(14 * 24 * 60 * 60) // 14일
                .build();
    }

    // Refresh Token 쿠키 삭제 (로그아웃 시 사용)
    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(0)
                .build();
    }
}
