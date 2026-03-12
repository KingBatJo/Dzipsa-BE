package com.example.dzipsa.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessSecret;
    private String refreshSecret;
    private long accessTokenExpiration = 1_800_000L;   // 30분
    private long refreshTokenExpiration = 604_800_000L; // 7일
}
