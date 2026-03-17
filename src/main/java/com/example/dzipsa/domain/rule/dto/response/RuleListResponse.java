package com.example.dzipsa.domain.rule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleListResponse {

    @Schema(description = "규칙 ID")
    private Long id;

    @Schema(description = "방 ID")
    private Long roomId;

    @Schema(description = "등록자 ID")
    private Long registerId;

    @Schema(description = "규칙 제목")
    private String title;

    @Schema(description = "메모")
    private String memo;

    @Schema(description = "시간 설정 여부")
    private boolean timeSettingEnabled;

    @Schema(description = "시작 시간")
    private LocalTime startTime;

    @Schema(description = "종료 시간")
    private LocalTime endTime;

    @Schema(description = "반복 설정 여부")
    private boolean repeatEnabled;

    @Schema(description = "반복 요일 목록 (1:월 ~ 7:일)", example = "1,3,5")
    private String repeatDays;

    @Schema(description = "집사에게 알리기 비활성화 여부 (24시간 내 이미 알린 경우 true)")
    private boolean warningDisabled;
}
