package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.todo.repository.TodoInstanceRepository;
import com.example.dzipsa.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoBatchService {

  private final TodoRepository todoRepository;
  private final TodoInstanceRepository todoInstanceRepository;

  /**
   * [매일 자정] 지연 데이터 체크만 수행
   */
  public void runDailyTodoBatch() {
    LocalDate today = LocalDate.now();
    long overdueCount = todoInstanceRepository.countByTargetDateBeforeAndStatus(today, TodoStatus.PENDING);
    log.info("[Daily Batch] 지연 건수 체크 완료: {}건", overdueCount);
  }

  /**
   * [주간/월간/수동] 지정된 기간만큼 할 일 인스턴스 생성
   * @param daysAhead 오늘부터 며칠 후까지 생성할 것인지 (예: 14일, 30일)
   */
  public void generateRecurringTodos(int daysAhead) {
    LocalDate today = LocalDate.now();
    LocalDate maxDate = today.plusDays(daysAhead);
    log.info("[Batch] 일정 생성 시작 (기간: {} ~ {})", today, maxDate);

    List<Todo> activeTodos = todoRepository.findAllByIsActiveTrue();
    int createdCount = 0;

    for (Todo todo : activeTodos) {
      createdCount += generateInstancesRange(todo, today, maxDate);
    }
    log.info("[Batch] 생성 완료: 총 {}건", createdCount);
  }

  /**
   * [공통 로직] 특정 할 일(Todo)에 대해 지정된 기간 내의 인스턴스를 생성
   * 서비스 레이어(생성 시점)와 배치 레이어에서 공통으로 사용
   */
  public int generateInstancesRange(Todo todo, LocalDate start, LocalDate end) {
    int count = 0;
    for (LocalDate targetDate = start; !targetDate.isAfter(end); targetDate = targetDate.plusDays(1)) {
      if (isInvalidDate(todo, targetDate)) continue;

      if (isRecurringDay(todo, targetDate)) {
        if (!todoInstanceRepository.existsByTodoIdAndTargetDate(todo.getId(), targetDate)) {
          createInstance(todo, targetDate);
          count++;
        }
      }
    }
    return count;
  }

  public String executeSmartBatch() {
    LocalDate today = LocalDate.now();
    int daysToGenerate = 1; // 기본은 당일 체크

    // 1. 매일 수행하는 지연 건 체크
    runDailyTodoBatch();

    // 2. 오늘이 월요일이면 14일치 생성
    if (today.getDayOfWeek().getValue() == 1) {
      daysToGenerate = 14;
    }

    // 3. 오늘이 1일이면 30일치 생성 (월요일보다 우선순위 높음)
    if (today.getDayOfMonth() == 1) {
      daysToGenerate = 30;
    }

    // 결정된 기간만큼 생성 실행
    generateRecurringTodos(daysToGenerate);

    return today + " 기준, " + daysToGenerate + "일치 배치가 완료되었습니다.";
  }

  // --- 헬퍼 메서드 ---
  private void createInstance(Todo todo, LocalDate date) {
    TodoInstance instance = TodoInstance.builder()
        .todo(todo)
        .room(todo.getRoom())
        .title(todo.getTitle())
        .memo(todo.getMemo())
        .targetDate(date)
        .status(TodoStatus.PENDING)
        .actualAssignee(todo.getDefaultAssignee())
        .build();
    todoInstanceRepository.save(instance);
  }

  private boolean isInvalidDate(Todo todo, LocalDate date) {
    return date.isBefore(todo.getStartDate()) || (todo.getEndDate() != null && date.isAfter(todo.getEndDate()));
  }

  private boolean isRecurringDay(Todo todo, LocalDate targetDate) {
    RecurringType type = todo.getRecurringType();

    // 반복 없음일 경우 시작일에만 생성
    if (type == RecurringType.NONE) {
      return targetDate.equals(todo.getStartDate());
    }

    if (type == RecurringType.WEEKLY) {
      int dayOfWeek = targetDate.getDayOfWeek().getValue();
      return todo.getRepeatDays() != null && todo.getRepeatDays().contains(String.valueOf(dayOfWeek));
    }

    if (type == RecurringType.MONTHLY) {
      if (todo.getRepeatDays() == null) return false;
      return String.valueOf(targetDate.getDayOfMonth()).equals(todo.getRepeatDays());
    }

    return false;
  }
}