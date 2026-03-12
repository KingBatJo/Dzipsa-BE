package com.example.dzipsa.domain.auth.converter;

import com.example.dzipsa.domain.auth.dto.response.TokenResponse;

import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    private static final String GRANT_TYPE = "Bearer";

    public TokenResponse toTokenResponse(String accessToken, String refreshToken, Long accessTokenExpiresIn) {
        return TokenResponse.builder()
                .grantType(GRANT_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .build();
    }
}
