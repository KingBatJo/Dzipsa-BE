package com.example.dzipsa.domain.todo.dto.request;

import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class TodoCreateRequest {
  @NotBlank(message = "제목은 필수입니다.")
  @Size(max = 30)
  private String title;

  @Size(max = 300)
  private String memo;

  private Long assigneeId; // 미기입 시 작성자 본인

  private Boolean isRandom;

  @NotNull(message = "반복 설정을 선택해주세요.")
  private RecurringType recurringType;

  private String repeatDays; // "0,2,4"

  @NotNull(message = "시작 날짜를 선택해주세요.")
  private LocalDate startDate;

  private LocalDate endDate;
}