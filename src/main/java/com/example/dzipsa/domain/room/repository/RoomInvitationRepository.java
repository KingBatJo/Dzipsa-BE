package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.RoomErrorCode;
import com.example.dzipsa.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RoomInvitationRepository {

    private final RedisUtil redisUtil;
    private static final String PREFIX = "invitation_code:";
    private static final long TTL_HOURS = 24;
    private static final int MAX_RETRY_COUNT = 5;

    public String createInvitationCode(Long roomId) {
        int retryCount = 0;
        String code;

        do {
            if (retryCount >= MAX_RETRY_COUNT) {
                throw new BusinessException(RoomErrorCode.INVITATION_CODE_GENERATION_FAILED);
            }

            code = generateCode();
            retryCount++;

        } while (redisUtil.hasKey(PREFIX + code));

        redisUtil.set(PREFIX + code, String.valueOf(roomId), TTL_HOURS, TimeUnit.HOURS);
        return code;
    }

    public Optional<Long> findRoomIdByCode(String code) {
        String roomIdStr = redisUtil.get(PREFIX + code);
        if (roomIdStr == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(roomIdStr));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
