package com.example.dzipsa.domain.rule.repository;

import com.example.dzipsa.domain.rule.entity.Rule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule, Long> {
    
    Optional<Rule> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT r FROM Rule r WHERE r.roomId = :roomId AND r.deletedAt IS NULL AND (:cursor IS NULL OR r.id < :cursor) ORDER BY r.id DESC")
    List<Rule> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("cursor") Long cursor, Pageable pageable);
}
