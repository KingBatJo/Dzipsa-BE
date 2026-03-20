package com.example.dzipsa.domain.todo.dto.request;

import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class TodoUpdateRequest {
  private String title;
  private String memo;
  private Boolean isRandom;
  private Long assigneeId;
  private RecurringType recurringType;
  private String repeatDays;
  private LocalDate startDate;
  private LocalDate endDate;
}