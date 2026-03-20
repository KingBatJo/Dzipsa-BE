package com.example.dzipsa.global.scheduler;

import com.example.dzipsa.domain.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoScheduler {

  private final TodoService todoService;

  // 매주 반복 - 월요일 오전 12시 실행 (향후 2주치 생성)
  @Scheduled(cron = "0 0 0 * * MON")
  public void runWeeklyBatch() {
    log.info("[Batch] 주간 반복 할 일 생성 시작 (월요일 자정)");
    todoService.generateRecurringTodos();
  }

  // 매월 반복 - 매월 1일 오전 12시 실행
  @Scheduled(cron = "0 0 0 1 * *")
  public void runMonthlyBatch() {
    log.info("[Batch] 월간 반복 할 일 생성 시작 (매월 1일 자정)");
    todoService.generateRecurringTodos();
  }
}