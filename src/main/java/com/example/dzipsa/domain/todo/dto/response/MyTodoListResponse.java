package com.example.dzipsa.domain.todo.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * [나의 할 일 통합 응답]
 * 지연/오늘/예정 세 파트 모두 무한 스크롤이 가능하도록 구조화
 */
@Getter
@Builder
public class MyTodoListResponse {
  // 1. 지연된 할 일 (마감일 지남)
  private PagedTodoResponse missedTodos;

  // 2. 오늘의 할 일
  private PagedTodoResponse todayTodos;

  // 3. 예정된 할 일 (내일 이후)
  private PagedTodoResponse upcomingTodos;

  @Getter
  @Builder
  public static class PagedTodoResponse {
    private List<TodoSummaryResponse> content;
    private Boolean hasNext;
    private String nextCursor; // "날짜_ID" 형태의 커서
  }
}