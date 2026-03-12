package com.example.dzipsa.global.security;

import com.example.dzipsa.domain.user.entity.enums.UserRole;
import com.example.dzipsa.global.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId, String email, UserRole role) {
        return createToken(
                userId,
                email,
                role,
                TOKEN_TYPE_ACCESS,
                jwtProperties.getAccessSecret(),
                jwtProperties.getAccessTokenExpiration()
        );
    }

    public String createRefreshToken(Long userId, String email, UserRole role) {
        return createToken(
                userId,
                email,
                role,
                TOKEN_TYPE_REFRESH,
                jwtProperties.getRefreshSecret(),
                jwtProperties.getRefreshTokenExpiration()
        );
    }

    private String createToken(Long userId, String email, UserRole role, String type,
                               String secret, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_USER_ID, userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromAccessToken(String token) {
        Claims claims = parseToken(token, jwtProperties.getAccessSecret());
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, jwtProperties.getAccessSecret(), TOKEN_TYPE_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, jwtProperties.getRefreshSecret(), TOKEN_TYPE_REFRESH);
    }

    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = parseToken(token, jwtProperties.getRefreshSecret());
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    private boolean validateToken(String token, String secret, String expectedType) {
        try {
            Claims claims = parseToken(token, secret);
            String type = claims.get(CLAIM_TYPE, String.class);
            return expectedType.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpiration();
    }

    // 토큰의 만료 시간을 Date 객체로 반환하는 메서드 추가
    public Date getExpirationDateFromAccessToken(String token) {
        return parseToken(token, jwtProperties.getAccessSecret()).getExpiration();
    }
}
