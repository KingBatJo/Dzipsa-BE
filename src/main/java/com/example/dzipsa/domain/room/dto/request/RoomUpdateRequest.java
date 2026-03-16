package com.example.dzipsa.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomUpdateRequest {

    @Schema(description = "수정할 방 이름", example = "행복한 우리집")
    private String name;

    @Schema(description = "수정할 가훈/목표", example = "건강이 최고다")
    private String motto;
}
