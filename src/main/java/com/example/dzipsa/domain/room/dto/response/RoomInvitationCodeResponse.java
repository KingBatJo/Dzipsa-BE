package com.example.dzipsa.domain.room.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RoomInvitationCodeResponse {
    
    @Schema(description = "방 초대 코드", example = "123456")
    private String invitationCode;

    @Schema(description = "재발급 가능 시간 (재발급 쿨다운이 없을 경우 현재 시간 이전으로 반환됨)", example = "2024-05-10T15:57:00")
    private LocalDateTime reissueAvailableAt;
}
