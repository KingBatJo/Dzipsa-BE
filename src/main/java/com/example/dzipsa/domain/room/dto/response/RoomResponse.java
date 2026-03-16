package com.example.dzipsa.domain.room.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String name;
    private String motto;
    private Long ownerId;
    private Long membersCount;
    private Long score;
    private String invitationCode;
    
    // 추후 다른 도메인 연동 시 사용할 필드들
    private String ruleWarningCount; // 방 전체 규칙 경고 수
    private String delayTaskCount; // 개인 지연 할 일 수

    // 방 구성원 리스트 (나 포함)
    private List<RoomMemberResponse> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
