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
    private static final String CODE_PREFIX = "invitation_code:";
    private static final String ROOM_PREFIX = "room_invitation:"; // roomId -> code 매핑
    private static final long TTL_HOURS = 24;
    private static final int MAX_RETRY_COUNT = 5;

    public String createInvitationCode(Long roomId) {
        // 이미 해당 방의 활성화된 초대 코드가 있는지 확인
        Optional<String> existingCode = findCodeByRoomId(roomId);
        // 기존 코드 재사용
        return existingCode.orElseGet(() -> generateAndSaveCode(roomId));

    }

    public String reissueInvitationCode(Long roomId) {
        // 1. 기존 코드가 있다면 삭제 (무효화)
        findCodeByRoomId(roomId).ifPresent(oldCode -> redisUtil.delete(CODE_PREFIX + oldCode));
        redisUtil.delete(ROOM_PREFIX + roomId);

        // 2. 새로운 코드 생성 및 반환
        return generateAndSaveCode(roomId);
    }

    public Optional<Long> findRoomIdByCode(String code) {
        String roomIdStr = redisUtil.get(CODE_PREFIX + code);
        if (roomIdStr == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(roomIdStr));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<String> findCodeByRoomId(Long roomId) {
        String code = redisUtil.get(ROOM_PREFIX + roomId);
        return Optional.ofNullable(code);
    }

    private String generateAndSaveCode(Long roomId) {
        int retryCount = 0;
        String code;

        do {
            if (retryCount >= MAX_RETRY_COUNT) {
                throw new BusinessException(RoomErrorCode.INVITATION_CODE_GENERATION_FAILED);
            }

            code = generateCode();
            retryCount++;

        } while (redisUtil.hasKey(CODE_PREFIX + code));

        // 양방향 매핑 저장 (코드 -> 방ID, 방ID -> 코드)
        redisUtil.set(CODE_PREFIX + code, String.valueOf(roomId), TTL_HOURS, TimeUnit.HOURS);
        redisUtil.set(ROOM_PREFIX + roomId, code, TTL_HOURS, TimeUnit.HOURS);
        
        return code;
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
