package com.example.dzipsa.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // Key Prefixes
    private static final String BLACKLIST_PREFIX = "blacklist:";


    // --- Blacklist (Auth) ---

    public void setBlackList(String accessToken, String value, long expirationMinutes) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken, 
                value, 
                expirationMinutes, 
                TimeUnit.MINUTES
        );
    }

    public boolean hasKeyBlackList(String accessToken) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken);
    }

    public Object getBlackList(String accessToken) {
        return redisTemplate.opsForValue().get(BLACKLIST_PREFIX + accessToken);
    }

}
