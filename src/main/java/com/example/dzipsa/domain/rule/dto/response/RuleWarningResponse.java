package com.example.dzipsa.domain.rule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleWarningResponse {

    @Schema(description = "경고 ID")
    private Long id;

    @Schema(description = "방 ID")
    private Long roomId;

    @Schema(description = "규칙 ID")
    private Long ruleId;

    @Schema(description = "규칙 제목")
    private String ruleTitle;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;
}
