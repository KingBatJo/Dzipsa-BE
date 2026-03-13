package com.example.dzipsa.domain.room.service;

import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.request.RoomUpdateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.user.entity.User;

import java.util.List;

public interface RoomService {
    RoomResponse create(RoomCreateRequest request, User user);
    RoomResponse updateMyRoom(RoomUpdateRequest request, User user); // 변경됨
    RoomResponse getRoom(Long roomId);
    RoomResponse joinRoom(String invitationCode, User user);
    void leaveRoom(Long roomId, User user);
    RoomResponse getMyRoom(User user);
    List<RoomMemberResponse> getRoomMembers(User user, boolean excludeMe);
}
