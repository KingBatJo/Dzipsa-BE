package com.example.dzipsa.domain.todo.dto.response;

import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class TodoSummaryResponse {
  private Long instanceId;
  private String title;
  private Long assigneeId;
  private String assigneeNickname;
  private String profileImageUrl;
  private TodoStatus status;
  private LocalDate targetDate;
  private Long delayDays;
  private String imageUrl;
}