package com.example.dzipsa.domain.room.scheduler;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.rule.repository.RuleWarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomScoreScheduler {

    private final RoomRepository roomRepository;
    private final RuleWarningRepository ruleWarningRepository;

    private static final int CHUNK_SIZE = 100;

    @Scheduled(cron = "0 0 0 * * *") // 매일 00시(자정)에 실행
    @Transactional
    public void resetRoomScores() {
        log.info("[RoomScoreScheduler] 00시 방 점수 리셋 시작");

        LocalDate today = LocalDate.now();
        int page = 0;
        int totalProcessed = 0;
        Page<Room> roomPage;

        do {
            roomPage = roomRepository.findByDeletedAtIsNull(PageRequest.of(page, CHUNK_SIZE));

            for (Room room : roomPage.getContent()) {
                // 시간(hour) 차이로 인한 누락을 방지하기 위해 LocalDate 기준으로 날짜 차이만 계산
                long daysSinceCreation = ChronoUnit.DAYS.between(room.getCreatedAt().toLocalDate(), today);
                if (daysSinceCreation > 0 && daysSinceCreation % 30 == 0) {
                    log.info("[RoomScoreScheduler] 방(roomId={}) 생성 30일 주기로 점수 리셋", room.getId());
                    room.resetScore();
                }
            }

            totalProcessed += roomPage.getNumberOfElements();
            page++;

        } while (roomPage.hasNext());

        log.info("[RoomScoreScheduler] 00시 방 점수 리셋 완료. 총 {}개 방 검사", totalProcessed);
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
    @Transactional
    public void increaseRoomScores() {
        log.info("[RoomScoreScheduler] 새벽 3시 방 점수 상승 업데이트 시작");

        LocalDate today = LocalDate.now();
        // 실행 시간과 무관하게 '어제 00:00:00'부터 '오늘 00:00:00' 직전까지를 기준으로 삼음
        LocalDateTime startOfYesterday = today.minusDays(1).atStartOfDay();
        LocalDateTime startOfToday = today.atStartOfDay();

        int page = 0;
        int totalProcessed = 0;
        Page<Room> roomPage;

        do {
            roomPage = roomRepository.findByDeletedAtIsNull(PageRequest.of(page, CHUNK_SIZE));

            for (Room room : roomPage.getContent()) {
                // 기준 시간을 명확히 지정하여 구간 조회 (어제 하루 종일 발생한 경고 횟수)
                int warningCount = ruleWarningRepository.countByRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    room.getId(), startOfYesterday, startOfToday);

                if (warningCount == 0) {
                    log.info("[RoomScoreScheduler] 방(roomId={}) 어제(24시간) 동안 경고 없어 점수 1점 상승", room.getId());
                    room.increaseScore(1.0);
                }
            }

            totalProcessed += roomPage.getNumberOfElements();
            page++;

        } while (roomPage.hasNext());

        log.info("[RoomScoreScheduler] 새벽 3시 방 점수 상승 업데이트 완료. 총 {}개 방 검사", totalProcessed);
    }
}