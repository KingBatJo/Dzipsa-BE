package com.example.dzipsa.domain.todo.repository;

import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodoInstanceRepository extends JpaRepository<TodoInstance, Long> {

  /**
   * [지연된 할 일 - 무한 스크롤]
   * 날짜와 ID를 조합한 복합 커서를 사용하여 데이터 누락을 방지
   * 정렬: 최신 날짜순 (DESC)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.status != 'COMPLETED' AND ti.targetDate < :today " +
      "AND (ti.targetDate < :cursorDate OR (ti.targetDate = :cursorDate AND ti.id < :cursorId)) " +
      "ORDER BY ti.targetDate DESC, ti.id DESC")
  Slice<TodoInstance> findMissedTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [오늘의 할 일 - 무한 스크롤]
   * 정렬: 등록 순서 (ASC)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.targetDate = :today " +
      "AND ti.id > :cursorId " +
      "ORDER BY ti.id ASC")
  Slice<TodoInstance> findTodayTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [예정된 할 일 - 무한 스크롤]
   * 정렬: 가까운 미래 순 (ASC)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.targetDate > :today " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findUpcomingTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  // 배치/로직용 유틸리티 메서드
  boolean existsByTodoIdAndTargetDate(Long todoId, LocalDate targetDate);
  List<TodoInstance> findAllByRoomIdAndTargetDateOrderByCreatedAtAsc(Long roomId, LocalDate today);
  Slice<TodoInstance> findAllByRoomIdAndStatusOrderByCreatedAtDesc(Long roomId, TodoStatus status, Pageable pageable);
  List<TodoInstance> findAllByTodoIdAndTargetDateAfter(Long todoId, LocalDate today);
}