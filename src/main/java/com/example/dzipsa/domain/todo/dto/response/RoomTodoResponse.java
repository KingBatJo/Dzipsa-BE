package com.example.dzipsa.domain.todo.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomTodoResponse {
  private TodoNudgeResponse nudgeInfo; // 넛지 가이드용 데이터
  private List<MemberTodoStatsResponse> memberStats; // 구성원별 오늘 남은 할 일 숫자
  private MyTodoListResponse.PagedTodoResponse todos; // 실제 할 일 목록

  @Getter
  @Builder
  public static class MemberTodoStatsResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private int remainingCount; // 해당 멤버의 오늘 남은 할 일 개수
  }
}