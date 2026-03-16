package com.example.dzipsa.domain.room.service;

import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.request.RoomUpdateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomInvitationCodeResponse;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomMottoResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.room.dto.response.UsedProfileImageResponse;
import com.example.dzipsa.domain.user.entity.User;

import java.util.List;

public interface RoomService {
    RoomResponse create(RoomCreateRequest request, User user);
    RoomResponse updateMyRoom(RoomUpdateRequest request, User user);
    RoomResponse getRoom(Long roomId);
    RoomResponse joinRoom(String invitationCode, User user);
    void leaveMyRoom(User user);
    RoomResponse getMyRoom(User user);
    List<RoomMemberResponse> getRoomMembers(User user, boolean excludeMe);
    
    RoomInvitationCodeResponse getInvitationCode(User user);
    RoomInvitationCodeResponse reissueInvitationCode(User user); // 재발급 추가
    void kickMember(Long memberUserId, User user);
    RoomMottoResponse getMotto(User user);
    UsedProfileImageResponse getUsedProfileImages(User user, String invitationCode);
}
