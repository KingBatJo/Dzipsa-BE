package com.example.dzipsa.domain.room.service;

import com.example.dzipsa.domain.room.converter.RoomConverter;
import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.request.RoomUpdateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomInvitationRepository;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.RoomErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomInvitationRepository roomInvitationRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RoomConverter roomConverter;

    @Override
    @Transactional
    public RoomResponse create(RoomCreateRequest request, User user) {
        // 이미 참여 중인 방이 있는지 확인
        if (roomMemberRepository.findByUserIdAndLeftAtIsNull(user.getId()).isPresent()) {
            throw new BusinessException(RoomErrorCode.ALREADY_HAS_ROOM);
        }

        Room newRoom = roomConverter.toRoom(request, user);
        roomRepository.save(newRoom);

        // 방장도 멤버로 추가
        RoomMember roomMember = RoomMember.builder()
                .roomId(newRoom.getId())
                .userId(user.getId())
                .build();
        roomMemberRepository.save(roomMember);

        // 초대 코드 생성 및 Redis 저장 (Repository 위임)
        String invitationCode = roomInvitationRepository.createInvitationCode(newRoom.getId());

        return roomConverter.toRoomResponse(newRoom, invitationCode);
    }

    @Override
    @Transactional
    public RoomResponse updateMyRoom(RoomUpdateRequest request, User user) {
        // 내가 참여 중인 방 멤버십 조회
        RoomMember myMember = roomMemberRepository.findByUserIdAndLeftAtIsNull(user.getId())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        Room room = findRoomById(myMember.getRoomId());
        validateRoomOwner(room, user.getId());

        if (request.getName() != null) {
            room.updateName(request.getName());
        }

        if (request.getMotto() != null) {
            room.updateMotto(request.getMotto());
        }

        return roomConverter.toRoomResponse(room, null);
    }

    @Override
    public RoomResponse getRoom(Long roomId) {
        Room room = findRoomById(roomId);
        return roomConverter.toRoomResponse(room, null);
    }

    @Override
    @Transactional
    public RoomResponse joinRoom(String invitationCode, User user) {
        // 이미 참여 중인 방이 있는지 확인
        if (roomMemberRepository.findByUserIdAndLeftAtIsNull(user.getId()).isPresent()) {
            throw new BusinessException(RoomErrorCode.ALREADY_HAS_ROOM);
        }

        // 초대 코드로 방 ID 조회 (Repository 위임)
        Long roomId = roomInvitationRepository.findRoomIdByCode(invitationCode)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.INVALID_INVITATION_CODE));

        Room room = findRoomById(roomId);

        // 중복 가입 체크 (이미 가입된 상태인지 - 위에서 이미 체크했지만 방어 코드)
        if (roomMemberRepository.findByRoomIdAndUserIdAndLeftAtIsNull(roomId, user.getId()).isPresent()) {
            throw new BusinessException(RoomErrorCode.ALREADY_ROOM_MEMBER);
        }

        // 멤버 추가
        RoomMember roomMember = RoomMember.builder()
                .roomId(roomId)
                .userId(user.getId())
                .build();
        roomMemberRepository.save(roomMember);

        // 멤버 수 증가
        room.increaseMemberCount();

        return roomConverter.toRoomResponse(room, invitationCode);
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, User user) {
        Room room = findRoomById(roomId);

        // 내가 방 멤버인지 확인
        RoomMember me = roomMemberRepository.findByRoomIdAndUserIdAndLeftAtIsNull(roomId, user.getId())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.NOT_ROOM_MEMBER));

        // 1. 방장인지 확인
        if (room.getOwnerId().equals(user.getId())) {
            // 현재 방에 남아있는 멤버들을 가입일 순으로 조회
            List<RoomMember> members = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(roomId);

            // 나 자신을 제외한 가장 오래된 멤버 찾기
            Optional<RoomMember> nextOwner = members.stream()
                    .filter(m -> !m.getUserId().equals(user.getId()))
                    .findFirst();

            if (nextOwner.isPresent()) {
                // 다음 방장에게 권한 위임
                room.changeOwner(nextOwner.get().getUserId());
            } else {
                // 남은 사람이 없으면 방 삭제 (나 혼자 있다가 나가는 경우)
                room.delete();
            }
        }

        // 2. 퇴장 처리 (Soft Delete)
        me.leave();
        room.decreaseMemberCount();
    }

    @Override
    public RoomResponse getMyRoom(User user) {
        // 내가 참여 중인 방 멤버십 조회 (1명당 1개 방만 허용)
        RoomMember myMember = roomMemberRepository.findByUserIdAndLeftAtIsNull(user.getId())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // roomId로 방 정보 조회
        Room myRoom = findRoomById(myMember.getRoomId());

        return roomConverter.toRoomResponse(myRoom, null);
    }

    @Override
    public List<RoomMemberResponse> getRoomMembers(User user, boolean excludeMe) {
        // 1. 내가 속한 방 찾기
        RoomMember myMember = roomMemberRepository.findByUserIdAndLeftAtIsNull(user.getId())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
        Long myRoomId = myMember.getRoomId();

        // 2. 방의 모든 멤버 ID 조회
        List<Long> memberUserIds = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(myRoomId)
                .stream()
                .map(RoomMember::getUserId)
                .collect(Collectors.toList());

        if (memberUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 멤버들의 User 정보 조회
        List<User> members = userRepository.findAllById(memberUserIds);

        // 4. 필터링 및 DTO 변환
        return members.stream()
                .filter(member -> !excludeMe || !member.getId().equals(user.getId()))
                .map(roomConverter::toRoomMemberResponse)
                .collect(Collectors.toList());
    }

    private Room findRoomById(Long roomId) {
        return roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
    }

    private void validateRoomOwner(Room room, Long userId) {
        if (!room.getOwnerId().equals(userId)) {
            throw new BusinessException(RoomErrorCode.FORBIDDEN_NOT_OWNER);
        }
    }
}
