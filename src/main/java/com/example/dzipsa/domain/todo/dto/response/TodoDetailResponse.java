package com.example.dzipsa.domain.todo.dto.response;

import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoDetailResponse {
  private Long todoId;
  private Long instanceId;
  private String title;
  private String memo;

  // 담당자 정보
  private Long assigneeId;
  private String assigneeNickname;
  private String profileImageUrl;

  // 반복 및 상태
  private LocalDate targetDate;
  private RecurringType recurringType;
  private String repeatDays;
  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean isRandom;

  // 상태 및 시간 데이터
  private TodoStatus status;
  private LocalDateTime completedAt;  // 완료 시점
  private Long delayDays; // 진행, 완료, 지연, 지연완료

  // 인증샷
  private String imageUrl; // 완료 시 등록된 사진 URL

  // 권한 제어 (본인 것인지 확인용)
  private boolean isOwner; // 본인 담당 여부
  private boolean isWriter; // 작성자 여부 (수정/삭제 권한용)
}