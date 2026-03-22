package com.example.dzipsa.domain.todo.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoNudgeResponse {
  private int totalRoomTodoCount;      // 구성원 전체 오늘 할 일
  private int completedRoomTodoCount;  // 구성원이 오늘 완료한 할 일
  private int myRemainingTodoCount;    // [사용자]의 남은 할 일 개수
}
