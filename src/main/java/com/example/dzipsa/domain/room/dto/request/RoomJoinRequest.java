package com.example.dzipsa.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomJoinRequest {

    @NotBlank(message = "초대 코드는 필수입니다.")
    @Schema(description = "초대 코드 (6자리)", example = "123456")
    private String invitationCode;
}
