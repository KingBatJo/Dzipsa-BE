package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum UserErrorCode implements ApiCode {

    // --- 4xx Client Errors ---

    // 400 Bad Request
    NICKNAME_DUPLICATION(HttpStatus.BAD_REQUEST.value(), 240001, "이미 사용 중인 닉네임입니다."),
    DUPLICATED_PROFILE_IMAGE(HttpStatus.BAD_REQUEST.value(), 240002, "방 구성원과 중복된 프로필 이미지입니다."),
    ALREADY_AGREED_TERMS(HttpStatus.BAD_REQUEST.value(), 240003, "이미 이용약관에 동의한 사용자입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 240401, "존재하지 않는 유저입니다.");

    private final Integer httpStatus;
    private final Integer code;
    private final String message;
}
