package com.example.dzipsa.domain.rule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class RuleCreateRequest {

    @NotBlank(message = "규칙 제목은 필수입니다.")
    @Size(max = 30, message = "규칙 제목은 최대 30자까지 가능합니다.")
    @Schema(description = "규칙 제목", example = "분리수거 하기")
    private String title;

    @Size(max = 300, message = "메모는 최대 300자까지 가능합니다.")
    @Schema(description = "메모", example = "매주 화요일 저녁")
    private String memo;

    @Schema(description = "시간 설정 여부", example = "true")
    private boolean timeSettingEnabled;

    @Schema(description = "시작 시간", example = "09:00:00")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "12:00:00")
    private LocalTime endTime;

    @Schema(description = "반복 설정 여부 (false: 매일, true: 설정 요일)", example = "false")
    private boolean repeatEnabled;

    @Schema(description = "반복 요일 목록 (1:월 ~ 7:일)", example = "1,3,5")
    private String repeatDays;

    @Schema(description = "알림 설정 여부", example = "true")
    private boolean notiEnabled;
}
