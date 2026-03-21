package com.example.dzipsa.domain.room.repository;

import com.example.dzipsa.domain.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 삭제되지 않은 방 조회
    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    // 삭제되지 않은 방 페이징 조회 (스케줄러용 최적화)
    Page<Room> findByDeletedAtIsNull(Pageable pageable);
}
