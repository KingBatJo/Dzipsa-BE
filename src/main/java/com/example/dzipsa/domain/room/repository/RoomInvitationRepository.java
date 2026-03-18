package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.RoomErrorCode;
import com.example.dzipsa.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RoomInvitationRepository {

    private final RedisUtil redisUtil;
    private static final String CODE_PREFIX = "invitation_code:";
    private static final String ROOM_PREFIX = "room_invitation:"; // roomId -> code 매핑
    private static final String TIME_PREFIX = "reissue_at:"; // 정확한 재발급 가능 시간 매핑
    private static final long TTL_HOURS = 24;
    private static final long COOLDOWN_HOURS = 1;
    private static final int MAX_RETRY_COUNT = 5;

    public String createInvitationCode(Long roomId) {
        // 이미 해당 방의 활성화된 초대 코드가 있는지 확인
        Optional<String> existingCode = findCodeByRoomId(roomId);
        // 기존 코드 재사용
        return existingCode.orElseGet(() -> generateAndSaveCode(roomId));

    }

    public String reissueInvitationCode(Long roomId) {
        // 쿨다운 확인 (재발급 가능 시간이 남았다면 예외 발생)
        if (getReissueCooldownSeconds(roomId) > 0) {
            throw new BusinessException(RoomErrorCode.REISSUE_COOLDOWN_ACTIVE);
        }

        // 1. 기존 코드가 있다면 삭제 (무효화)
        findCodeByRoomId(roomId).ifPresent(oldCode -> redisUtil.delete(CODE_PREFIX + oldCode));
        redisUtil.delete(ROOM_PREFIX + roomId);
        redisUtil.delete(TIME_PREFIX + roomId);

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

    /**
     * 재발급 가능까지 남은 시간을 초(Seconds) 단위로 반환합니다. (쿨다운 확인용)
     */
    public Long getReissueCooldownSeconds(Long roomId) {
        Long expire = redisUtil.getExpire(ROOM_PREFIX + roomId);
        
        if (expire == null || expire <= 0) {
            return 0L;
        }

        // 전체 TTL(24시간) 중 남은 시간이 23시간(24-1)보다 크면 아직 쿨다운(1시간) 기간임
        long cooldownThresholdSeconds = (TTL_HOURS - COOLDOWN_HOURS) * 3600;
        
        if (expire > cooldownThresholdSeconds) {
            return expire - cooldownThresholdSeconds;
        }
        
        return 0L;
    }

    /**
     * 정확한 재발급 가능 시간(LocalDateTime)을 계산하여 반환합니다. (생성 시간 + 1시간)
     */
    public LocalDateTime getReissueAvailableAt(Long roomId) {
        // 1. Redis에 저장된 절대 시간이 있다면 반환
        String storedTime = redisUtil.get(TIME_PREFIX + roomId);
        if (storedTime != null) {
            // 이전에 LocalTime으로 저장된 데이터가 있을 경우를 대비한 안전한 파싱
            try {
                return LocalDateTime.parse(storedTime);
            } catch (Exception e) {
                // 파싱 실패시 아래 폴백으로 넘어감
            }
        }

        // 2. 기존 코드를 위한 폴백
        Long expire = redisUtil.getExpire(ROOM_PREFIX + roomId);
        if (expire == null || expire <= 0) {
            return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS); // 만료되었거나 없으면 현재 시간
        }

        long offsetSeconds = expire - ((TTL_HOURS - COOLDOWN_HOURS) * 3600);
        return LocalDateTime.now().plusSeconds(offsetSeconds).truncatedTo(ChronoUnit.SECONDS);
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
        
        // 재발급 가능 시간 저장
        LocalDateTime availableAt = LocalDateTime.now().plusHours(COOLDOWN_HOURS).truncatedTo(ChronoUnit.SECONDS);
        redisUtil.set(TIME_PREFIX + roomId, availableAt.toString(), TTL_HOURS, TimeUnit.HOURS);
        
        return code;
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
