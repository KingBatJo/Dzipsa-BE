package com.example.dzipsa.domain.rule.converter;

import com.example.dzipsa.domain.rule.dto.request.RuleCreateRequest;
import com.example.dzipsa.domain.rule.dto.response.RuleListResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleWarningResponse;
import com.example.dzipsa.domain.rule.entity.Rule;
import com.example.dzipsa.domain.rule.entity.RuleWarning;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class RuleConverter {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(0, 0); // 24:00 is 00:00

    public Rule toRule(RuleCreateRequest request, Long roomId, Long userId) {
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        if (request.isTimeSettingEnabled()) {
            if (startTime == null) startTime = DEFAULT_START_TIME;
            if (endTime == null) endTime = DEFAULT_END_TIME;
        } else {
            startTime = null;
            endTime = null;
        }

        String repeatDays = request.isRepeatEnabled() ? normalizeRepeatDays(request.getRepeatDays()) : null;

        return Rule.builder()
                .roomId(roomId)
                .registerId(userId)
                .title(request.getTitle())
                .memo(request.getMemo())
                .timeSettingEnabled(request.isTimeSettingEnabled())
                .startTime(startTime)
                .endTime(endTime)
                .repeatEnabled(request.isRepeatEnabled())
                .repeatDays(repeatDays)
                .notiEnabled(request.isNotiEnabled())
                .build();
    }

    public RuleResponse toRuleResponse(Rule rule, boolean isWarningDisabled, int totalWarningCount) {
        return RuleResponse.builder()
                .id(rule.getId())
                .roomId(rule.getRoomId())
                .registerId(rule.getRegisterId())
                .title(rule.getTitle())
                .memo(rule.getMemo())
                .timeSettingEnabled(rule.isTimeSettingEnabled())
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .repeatEnabled(rule.isRepeatEnabled())
                .repeatDays(rule.getRepeatDays())
                .notiEnabled(rule.isNotiEnabled())
                .warningDisabled(isWarningDisabled)
                .totalWarningCount(totalWarningCount)
                .build();
    }

    public RuleListResponse toRuleListResponse(Rule rule, boolean isWarningDisabled) {
        return RuleListResponse.builder()
                .id(rule.getId())
                .roomId(rule.getRoomId())
                .registerId(rule.getRegisterId())
                .title(rule.getTitle())
                .memo(rule.getMemo())
                .timeSettingEnabled(rule.isTimeSettingEnabled())
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .repeatEnabled(rule.isRepeatEnabled())
                .repeatDays(rule.getRepeatDays())
                .warningDisabled(isWarningDisabled)
                .build();
    }

    public String normalizeRepeatDays(String repeatDays) {
        if (repeatDays == null || repeatDays.isBlank()) {
            return null;
        }
        return Arrays.stream(repeatDays.split(","))
                .map(String::trim)
                .filter(day -> !day.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    public RuleWarningResponse toRuleWarningResponse(RuleWarning warning, String ruleTitle) {
        return RuleWarningResponse.builder()
                .id(warning.getId())
                .roomId(warning.getRoomId())
                .ruleId(warning.getRuleId())
                .ruleTitle(ruleTitle)
                .createdAt(warning.getCreatedAt())
                .build();
    }
}
