package com.example.dzipsa.global.scheduler;

import com.example.dzipsa.domain.todo.service.TodoBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoScheduler {

  private final TodoBatchService todoBatchService;

  /**
   * 매일 자정 실행 - 지연 건 관리
   */
  @Scheduled(cron = "0 0 0 * * *")
  public void runDailyBatch() {
    log.info("[Batch] 일일 상태 관리 시작");
    todoBatchService.runDailyTodoBatch();
  }

  /**
   * 매주 월요일 자정 - 향후 2주치(14일) 생성
   */
  @Scheduled(cron = "0 0 0 * * MON")
  public void runWeeklyBatch() {
    log.info("[Batch] 주간 반복 할 일 생성 시작 (월요일 자정)");
    todoBatchService.generateRecurringTodos(14); // 14일치 생성
  }

  /**
   * 매월 1일 자정 - 향후 한 달치(30일) 생성
   */
  @Scheduled(cron = "0 0 0 1 * *")
  public void runMonthlyBatch() {
    log.info("[Batch] 월간 반복 할 일 생성 시작 (매월 1일 자정)");
    todoBatchService.generateRecurringTodos(30); // 30일치 생성
  }
}