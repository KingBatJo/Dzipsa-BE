package com.example.dzipsa.domain.todo.dto.request;

import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class TodoCreateRequest {
  @NotBlank(message = "할 일을 입력하세요")
  @Size(max = 30, message = "제목은 30자 이내로 입력해주세요.")
  private String title;

  // 1. 반복 OFF 시: 사용자가 직접 선택하는 날짜
  private LocalDate targetDate;

  // 2. 반복 ON 시: 시스템이 자동 계산
  private RecurringType recurringType; // NONE, WEEKLY, MONTHLY
  private String repeatDays;           // 요일(1~7) 또는 일자(1~31)
  private LocalDate startDate;         // 반복 시작일
  private LocalDate endDate;           // 반복 종료일 (없음 가능)

  // 공통 항목
  private Long assigneeId;
  private Boolean isRandom;

  @Size(max = 300, message = "메모는 300자 이내로 입력해주세요.")
  private String memo;
}