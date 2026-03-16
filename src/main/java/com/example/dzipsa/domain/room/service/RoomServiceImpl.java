package com.example.dzipsa.domain.room.service;

import com.example.dzipsa.domain.room.converter.RoomConverter;
import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.request.RoomUpdateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomInvitationCodeResponse;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomMottoResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.room.dto.response.UsedProfileImageResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("[RoomService] 방 생성 요청. userId={}", user.getId());
        validateUserNotInAnyRoom(user.getId());

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
        log.info("[RoomService] 방 생성 완료. roomId={}, invitationCode={}", newRoom.getId(), invitationCode);

        // 생성 직후에는 나 혼자이므로 내 정보만 포함
        List<RoomMemberResponse> members = List.of(roomConverter.toRoomMemberResponse(user));

        return roomConverter.toRoomResponse(newRoom, invitationCode, members);
    }

    @Override
    @Transactional
    public RoomResponse updateMyRoom(RoomUpdateRequest request, User user) {
        log.info("[RoomService] 방 정보 수정 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());
        Room room = findRoomById(myMember.getRoomId());
        
        validateRoomOwner(room, user.getId());

        if (request.getName() != null) {
            room.updateName(request.getName());
        }

        if (request.getMotto() != null) {
            room.updateMotto(request.getMotto());
        }

        log.info("[RoomService] 방 정보 수정 완료. roomId={}", room.getId());
        
        // 정보 수정 후에도 구성원 목록 포함해서 반환
        List<RoomMemberResponse> members = getRoomMembers(user, false);
        return roomConverter.toRoomResponse(room, null, members);
    }

    @Override
    public RoomResponse getRoom(Long roomId) {
        log.info("[RoomService] 방 단건 조회 요청. roomId={}", roomId);
        Room room = findRoomById(roomId);
        return roomConverter.toRoomResponse(room, null);
    }

    @Override
    @Transactional
    public RoomResponse joinRoom(String invitationCode, User user) {
        log.info("[RoomService] 방 입장 요청. userId={}, invitationCode={}", user.getId(), invitationCode);
        validateUserNotInAnyRoom(user.getId());

        // 초대 코드로 방 ID 조회 (Repository 위임)
        Long roomId = roomInvitationRepository.findRoomIdByCode(invitationCode)
                .orElseThrow(() -> {
                    log.warn("[RoomService] 유효하지 않은 초대 코드. code={}", invitationCode);
                    return new BusinessException(RoomErrorCode.INVALID_INVITATION_CODE);
                });

        Room room = findRoomById(roomId);

        // 최대 인원 수 확인 (6명 제한)
        if (room.getMembersCount() >= 6) {
            log.warn("[RoomService] 방 최대 인원 초과. roomId={}", roomId);
            throw new BusinessException(RoomErrorCode.ROOM_IS_FULL);
        }

        // 중복 가입 체크 (이미 가입된 상태인지 - 위에서 이미 체크했지만 방어 코드)
        if (roomMemberRepository.findByRoomIdAndUserIdAndLeftAtIsNull(roomId, user.getId()).isPresent()) {
            log.warn("[RoomService] 이미 해당 방의 멤버임. roomId={}, userId={}", roomId, user.getId());
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
        
        log.info("[RoomService] 방 입장 완료. roomId={}, userId={}", roomId, user.getId());

        // 입장 후 방 구성원 목록 조회 (나 포함)
        List<RoomMemberResponse> members = getRoomMembers(user, false);

        return roomConverter.toRoomResponse(room, invitationCode, members);
    }

    @Override
    @Transactional
    public void leaveMyRoom(User user) {
        log.info("[RoomService] 방 나가기 요청. userId={}", user.getId());
        RoomMember me = getActiveRoomMember(user.getId());
        Room room = findRoomById(me.getRoomId());

        // 1. 방장인지 확인
        if (room.getOwnerId().equals(user.getId())) {
            // 현재 방에 남아있는 멤버들을 가입일 순으로 조회
            List<RoomMember> members = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(room.getId());

            // 나 자신을 제외한 가장 오래된 멤버 찾기
            Optional<RoomMember> nextOwner = members.stream()
                    .filter(m -> !m.getUserId().equals(user.getId()))
                    .findFirst();

            if (nextOwner.isPresent()) {
                // 다음 방장에게 권한 위임
                room.changeOwner(nextOwner.get().getUserId());
                log.info("[RoomService] 방장 권한 위임. roomId={}, newOwnerId={}", room.getId(), nextOwner.get().getUserId());
            } else {
                // 남은 사람이 없으면 방 삭제 (나 혼자 있다가 나가는 경우)
                room.delete();
                log.info("[RoomService] 남은 멤버가 없어 방 삭제. roomId={}", room.getId());
            }
        }

        // 2. 퇴장 처리 (Soft Delete)
        me.leave();
        room.decreaseMemberCount();
        log.info("[RoomService] 방 나가기 완료. roomId={}, userId={}", room.getId(), user.getId());
    }

    @Override
    public RoomResponse getMyRoom(User user) {
        log.info("[RoomService] 내 방 조회 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());
        Room myRoom = findRoomById(myMember.getRoomId());

        // 현재 활성화된 초대 코드 조회 (만료되었으면 null 반환)
        String invitationCode = roomInvitationRepository.findCodeByRoomId(myMember.getRoomId()).orElse(null);

        // 방 구성원 목록 조회 (나 포함)
        List<RoomMemberResponse> members = getRoomMembers(user, false);

        return roomConverter.toRoomResponse(myRoom, invitationCode, members);
    }

    @Override
    public List<RoomMemberResponse> getRoomMembers(User user, boolean excludeMe) {
        log.info("[RoomService] 방 구성원 목록 조회 요청. userId={}, excludeMe={}", user.getId(), excludeMe);
        RoomMember myMember = getActiveRoomMember(user.getId());
        Long myRoomId = myMember.getRoomId();

        // 방의 모든 멤버 ID 조회
        List<Long> memberUserIds = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(myRoomId)
                .stream()
                .map(RoomMember::getUserId)
                .collect(Collectors.toList());

        if (memberUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 멤버들의 User 정보 조회
        List<User> members = userRepository.findAllById(memberUserIds);

        // 필터링 및 DTO 변환
        return members.stream()
                .filter(member -> !excludeMe || !member.getId().equals(user.getId()))
                .map(roomConverter::toRoomMemberResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomInvitationCodeResponse getInvitationCode(User user) {
        log.info("[RoomService] 초대 코드 조회 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());

        // Redis에서 방 ID로 코드를 찾고, 없으면 예외 발생 (자동 생성 안 함)
        String code = roomInvitationRepository.findCodeByRoomId(myMember.getRoomId())
                .orElseThrow(() -> {
                    log.warn("[RoomService] 만료되었거나 존재하지 않는 초대 코드. roomId={}", myMember.getRoomId());
                    return new BusinessException(RoomErrorCode.INVALID_INVITATION_CODE);
                });

        return new RoomInvitationCodeResponse(code);
    }

    @Override
    @Transactional
    public RoomInvitationCodeResponse reissueInvitationCode(User user) {
        log.info("[RoomService] 초대 코드 재발급 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());

        // 기존 코드를 삭제하고 완전히 새로운 코드를 생성하여 반환함
        String code = roomInvitationRepository.reissueInvitationCode(myMember.getRoomId());
        log.info("[RoomService] 초대 코드 재발급 완료. roomId={}, newCode={}", myMember.getRoomId(), code);
        
        return new RoomInvitationCodeResponse(code);
    }

    @Override
    @Transactional
    public void kickMember(Long memberUserId, User user) {
        log.info("[RoomService] 구성원 내보내기 요청. targetUserId={}, byUserId={}", memberUserId, user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());
        Room room = findRoomById(myMember.getRoomId());

        // 권한 확인 (방장만 강퇴 가능)
        validateRoomOwner(room, user.getId());

        // 자기 자신을 강퇴할 수 없음
        if (user.getId().equals(memberUserId)) {
            log.warn("[RoomService] 자기 자신을 내보낼 수 없음. userId={}", user.getId());
            throw new BusinessException(RoomErrorCode.CANNOT_KICK_SELF);
        }

        // 강퇴할 대상 멤버 찾기
        RoomMember targetMember = roomMemberRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), memberUserId)
                .orElseThrow(() -> {
                    log.warn("[RoomService] 내보낼 대상이 방의 멤버가 아님. targetUserId={}, roomId={}", memberUserId, room.getId());
                    return new BusinessException(RoomErrorCode.NOT_ROOM_MEMBER);
                });

        // 퇴장 처리
        targetMember.leave();
        room.decreaseMemberCount();
        log.info("[RoomService] 구성원 내보내기 완료. targetUserId={}, roomId={}", memberUserId, room.getId());
    }

    @Override
    public RoomMottoResponse getMotto(User user) {
        log.info("[RoomService] 가훈 조회 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());
        Room room = findRoomById(myMember.getRoomId());
        
        return new RoomMottoResponse(room.getMotto());
    }

    @Override
    public UsedProfileImageResponse getUsedProfileImages(User user, String invitationCode) {
        log.info("[RoomService] 방 사용 중인 프로필 이미지 조회 요청. userId={}, invitationCode={}", user.getId(), invitationCode);
        Long roomId;

        if (invitationCode != null && !invitationCode.isBlank()) {
            roomId = roomInvitationRepository.findRoomIdByCode(invitationCode)
                    .orElseThrow(() -> {
                        log.warn("[RoomService] 유효하지 않은 초대 코드. code={}", invitationCode);
                        return new BusinessException(RoomErrorCode.INVALID_INVITATION_CODE);
                    });
        } else {
            RoomMember myMember = getActiveRoomMember(user.getId());
            roomId = myMember.getRoomId();
        }

        // 방의 모든 멤버 ID 조회
        List<Long> memberUserIds = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)
                .stream()
                .map(RoomMember::getUserId)
                .collect(Collectors.toList());

        if (memberUserIds.isEmpty()) {
            return new UsedProfileImageResponse(Collections.emptyList());
        }

        // 멤버들의 User 정보 조회하여 프로필 이미지 추출
        List<User> members = userRepository.findAllById(memberUserIds);
        List<String> usedImages = members.stream()
                .map(User::getProfileImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());

        return new UsedProfileImageResponse(usedImages);
    }

    private void validateUserNotInAnyRoom(Long userId) {
        if (roomMemberRepository.findByUserIdAndLeftAtIsNull(userId).isPresent()) {
            log.warn("[RoomService] 이미 참여 중인 방이 존재함. userId={}", userId);
            throw new BusinessException(RoomErrorCode.ALREADY_HAS_ROOM);
        }
    }

    private RoomMember getActiveRoomMember(Long userId) {
        return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId)
                .orElseThrow(() -> {
                    log.warn("[RoomService] 사용자가 속한 방을 찾을 수 없음. userId={}", userId);
                    return new BusinessException(RoomErrorCode.ROOM_NOT_FOUND);
                });
    }

    private Room findRoomById(Long roomId) {
        return roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> {
                    log.warn("[RoomService] 방을 찾을 수 없음. roomId={}", roomId);
                    return new BusinessException(RoomErrorCode.ROOM_NOT_FOUND);
                });
    }

    private void validateRoomOwner(Room room, Long userId) {
        if (!room.getOwnerId().equals(userId)) {
            log.warn("[RoomService] 방장 권한 없음. roomId={}, userId={}", room.getId(), userId);
            throw new BusinessException(RoomErrorCode.FORBIDDEN_NOT_OWNER);
        }
    }
}
