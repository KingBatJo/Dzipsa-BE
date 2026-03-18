package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RoomErrorCode implements ApiCode {
    // --- 4xx Client Errors ---

    // 400 Bad Request
    INVALID_INVITATION_CODE(HttpStatus.BAD_REQUEST.value(), 340001, "유효하지 않은 초대 코드입니다."),
    ALREADY_ROOM_MEMBER(HttpStatus.BAD_REQUEST.value(), 340002, "이미 해당 방에 참여 중입니다."),
    ALREADY_HAS_ROOM(HttpStatus.BAD_REQUEST.value(), 340003, "이미 참여 중인 방이 있습니다."),
    ROOM_IS_FULL(HttpStatus.BAD_REQUEST.value(), 340004, "방의 최대 인원(6명)을 초과할 수 없습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST.value(), 340005, "자기 자신을 내보낼 수 없습니다."),
    REISSUE_COOLDOWN_ACTIVE(HttpStatus.BAD_REQUEST.value(), 340006, "초대 코드 재발급은 1시간에 한 번만 가능합니다."),

    // 403 Forbidden
    FORBIDDEN_NOT_OWNER(HttpStatus.FORBIDDEN.value(), 340301, "방장만 이 작업을 수행할 수 있습니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN.value(), 340302, "해당 방의 멤버가 아닙니다."),

    // 404 Not Found
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 340401, "해당 방을 찾을 수 없습니다."),


    // --- 5xx Server Errors ---

    // 500 Internal Server Error
    INVITATION_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), 350001, "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");


    private final Integer httpStatus;
    private final Integer code;
    private final String message;
}
