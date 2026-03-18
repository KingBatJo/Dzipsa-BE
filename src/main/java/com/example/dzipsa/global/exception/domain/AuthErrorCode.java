package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum AuthErrorCode implements ApiCode {

    // --- 4xx Client Errors ---

    // 400 Bad Request
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST.value(), 140001, "이미 존재하는 이메일입니다."),

    // 401 Unauthorized
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED.value(), 140101, "아이디 또는 비밀번호가 잘못되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED.value(), 140102, "유효하지 않은 리프레시 토큰입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED.value(), 140103, "로그인이 필요합니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 140401, "존재하지 않는 유저입니다.");

    private final Integer httpStatus;
    private final Integer code;
    private final String message;
}
