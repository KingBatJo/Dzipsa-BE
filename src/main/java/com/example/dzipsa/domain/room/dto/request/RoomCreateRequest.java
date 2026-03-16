package com.example.dzipsa.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {

    @Schema(description = "방 이름", example = "우리 가족 딥사")
    private String name;

    @Schema(description = "가훈 또는 목표", example = "화목한 우리집")
    private String motto;
}
