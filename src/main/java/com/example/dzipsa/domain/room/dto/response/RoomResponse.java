package com.example.dzipsa.domain.room.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String name;
    private String motto;
    private Long ownerId;
    private Long membersCount;
    private String invitationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
