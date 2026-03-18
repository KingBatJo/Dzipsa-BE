package com.example.dzipsa.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
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

    // --- General ---

    public void set(String key, String value, long expirationTime, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, expirationTime, timeUnit);
    }

    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 특정 Key의 남은 유효 시간을 반환합니다.
     * @param key Redis Key
     * @param timeUnit 반환받을 시간 단위 (예: TimeUnit.SECONDS, TimeUnit.MILLISECONDS)
     * @return 남은 시간 (Key가 존재하지 않으면 -2, 만료 시간이 설정되지 않았으면 -1 반환)
     */
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * 특정 Key의 남은 유효 시간을 초(Seconds) 단위로 반환합니다.
     * @param key Redis Key
     * @return 남은 시간(초)
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
