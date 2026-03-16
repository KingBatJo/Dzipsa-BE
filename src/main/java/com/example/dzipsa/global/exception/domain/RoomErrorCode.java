package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RoomErrorCode implements ApiCode {
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 40401, "해당 방을 찾을 수 없습니다."),
    FORBIDDEN_NOT_OWNER(HttpStatus.FORBIDDEN.value(), 40301, "방장만 이 작업을 수행할 수 있습니다."),
    INVALID_INVITATION_CODE(HttpStatus.BAD_REQUEST.value(), 40001, "유효하지 않은 초대 코드입니다."),
    INVITATION_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), 50001, "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN.value(), 40302, "해당 방의 멤버가 아닙니다."),
    ALREADY_ROOM_MEMBER(HttpStatus.BAD_REQUEST.value(), 40002, "이미 해당 방에 참여 중입니다."),
    ALREADY_HAS_ROOM(HttpStatus.BAD_REQUEST.value(), 40003, "이미 참여 중인 방이 있습니다."),
    ROOM_IS_FULL(HttpStatus.BAD_REQUEST.value(), 40004, "방의 최대 인원(6명)을 초과할 수 없습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST.value(), 40005, "자기 자신을 내보낼 수 없습니다.");

    private final Integer httpStatus;
    private final Integer code;
    private final String message;
}
