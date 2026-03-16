package com.example.dzipsa.domain.room.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomMemberResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
}
