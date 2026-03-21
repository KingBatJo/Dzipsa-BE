package com.example.dzipsa.domain.room.controller;

import com.example.dzipsa.domain.room.scheduler.RoomScoreScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Rooms", description = "관리자 전용 방 스케줄러 관리 API")
@RestController
@RequestMapping("/api/admin/rooms/scheduler")
@RequiredArgsConstructor
public class AdminRoomController {

    private final RoomScoreScheduler roomScoreScheduler;

    @PostMapping("/reset-scores")
    @Operation(summary = "방 점수 리셋 스케줄러 수동 실행", description = "생성 후 30일이 지난 방의 점수를 수동으로 3.0으로 초기화합니다.")
    public ResponseEntity<String> runResetScoresScheduler() {
        roomScoreScheduler.resetRoomScores();
        return ResponseEntity.ok("방 점수 리셋 스케줄러가 수동으로 실행되었습니다.");
    }

    @PostMapping("/increase-scores")
    @Operation(summary = "방 점수 상승 스케줄러 수동 실행", description = "최근 24시간 동안 경고가 없는 방의 점수를 수동으로 상승시킵니다.")
    public ResponseEntity<String> runIncreaseScoresScheduler() {
        roomScoreScheduler.increaseRoomScores();
        return ResponseEntity.ok("방 점수 상승 스케줄러가 수동으로 실행되었습니다.");
    }
}
