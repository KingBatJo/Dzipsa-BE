package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RuleErrorCode implements ApiCode {
    RULE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 40410, "해당 규칙을 찾을 수 없습니다."),
    FORBIDDEN_NOT_OWNER(HttpStatus.FORBIDDEN.value(), 40310, "방장만 규칙을 관리할 수 있습니다."),
    ALREADY_WARNED(HttpStatus.BAD_REQUEST.value(), 40010, "이미 24시간 내에 알리기를 수행한 규칙입니다."),
    NOTI_NOT_AVAILABLE_WITHOUT_TIME(HttpStatus.BAD_REQUEST.value(), 40011, "시간 설정이 없는 경우 알림 기능을 사용할 수 없습니다.");

    private final Integer httpStatus;
    private final Integer code;
    private final String message;
}
