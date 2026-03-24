package com.example.dzipsa.domain.todo.dto.response;

import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse.MemberTodoStatsResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoNudgeResponse {
  // 넛지용
  private int totalRoomTodoCount;      // 구성원 전체 오늘 할 일
  private int completedRoomTodoCount;  // 구성원이 오늘 완료한 할 일
  private int myRemainingTodoCount;    // [사용자]의 남은 할 일 개수

  // 메인 섹션 숫자용
  private int todayTotalCount;         // 오늘 할 일 n건
  private int delayedTotalCount;       // 지연된 할 일 n건
  private int allTotalCount;           // 모든 할 일 n건

  // 구성원별 오늘 남은 할 일 숫자
  private List<MemberTodoStatsResponse> memberStats;
}
