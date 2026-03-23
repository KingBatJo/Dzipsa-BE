package com.example.dzipsa.domain.todo.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomTodoResponse {
  private TodoNudgeResponse nudgeInfo; // 넛지 가이드용 데이터
  private List<TodoSummaryResponse> todos; // 실제 할 일 목록
}