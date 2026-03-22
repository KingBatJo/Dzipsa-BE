package com.example.dzipsa.domain.todo.dto.response;

import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class TodoCreateResponse {
  private Long todoId;
  private Long instanceId;
  private String title;
  private String memo;
  private Long assigneeId;
  private String assigneeNickname;
  private Boolean isRandom;
  private RecurringType recurringType;
  private String repeatDays;
  private LocalDate startDate;
  private LocalDate endDate;
}