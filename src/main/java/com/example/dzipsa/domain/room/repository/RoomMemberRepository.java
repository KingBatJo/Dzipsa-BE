package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.domain.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    // 현재 방에 참여 중인 멤버 조회 (가입일 오름차순) - 방장 위임용
    List<RoomMember> findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(Long roomId);

    // 내가 특정 방에 참여 중인지 확인
    Optional<RoomMember> findByRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);

    // 내가 참여 중인 방 멤버십 조회 (1명당 1개 방만 허용)
    Optional<RoomMember> findByUserIdAndLeftAtIsNull(Long userId);

    // 특정 방의 멤버 수 조회
    long countByRoomIdAndLeftAtIsNull(Long roomId);
}
