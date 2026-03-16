package com.example.dzipsa.domain.room.converter;

import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoomConverter {

    public Room toRoom(RoomCreateRequest request, User user) {
        return Room.builder()
                .name(request.getName())
                .motto(request.getMotto())
                .ownerId(user.getId())
                .build();
    }

    public RoomResponse toRoomResponse(Room room, String invitationCode) {
        return toRoomResponse(room, invitationCode, null);
    }

    public RoomResponse toRoomResponse(Room room, String invitationCode, List<RoomMemberResponse> members) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .motto(room.getMotto())
                .ownerId(room.getOwnerId())
                .membersCount(room.getMembersCount())
                .score(room.getScore())
                .invitationCode(invitationCode)
                .members(members)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public RoomMemberResponse toRoomMemberResponse(User user) {
        return RoomMemberResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
