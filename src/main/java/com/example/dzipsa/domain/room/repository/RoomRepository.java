package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 삭제되지 않은 방 조회
    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

}
