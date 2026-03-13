package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 내가 생성한 방 목록 (삭제되지 않은 방)
    List<Room> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    // 삭제되지 않은 방 조회
    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    // 방 ID 목록으로 삭제되지 않은 방들 조회 (내 방 목록 조회용)
    List<Room> findByIdInAndDeletedAtIsNull(List<Long> ids);
}
