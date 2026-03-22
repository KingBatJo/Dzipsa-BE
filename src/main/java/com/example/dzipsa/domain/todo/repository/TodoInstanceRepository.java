package com.example.dzipsa.domain.todo.repository;

import com.example.dzipsa.domain.todo.entity.Todo;
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
   * [나의 할 일 - 지연된 할 일 - 무한 스크롤]
   * 날짜와 ID를 조합한 복합 커서를 사용하여 데이터 누락을 방지
   * 정렬: 오래된 날짜순 (ASC)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.status != 'COMPLETED' AND ti.targetDate < :today " +
      "AND (ti.targetDate < :cursorDate OR (ti.targetDate = :cursorDate AND ti.id < :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findMissedTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [나의 할 일 - 오늘의 할 일 - 무한 스크롤]
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
   * [나의 할 일 - 예정된 할 일 - 무한 스크롤]
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

  /**
   * [우리집 할 일 - 오늘 할 일]
   * 상태 순(PENDING 우선) -> 생성일 순 정렬
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId AND ti.targetDate = :today " +
      "ORDER BY CASE WHEN ti.status = 'PENDING' THEN 0 ELSE 1 END ASC, ti.createdAt ASC")
  List<TodoInstance> findRoomTodayTodos(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today);

  /**
   * [우리집 할 일 - 지연된 할 일]
   * 오늘 이전 날짜 + 미완료 + 오래된 날짜순(D+5, D+3...)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate < :today AND ti.status = :status " +
      "ORDER BY ti.targetDate ASC")
  List<TodoInstance> findRoomDelayedTodos(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status);

  /**
   * [우리집 할 일 - 모든 할 일]
   * 정렬 1순위 마감일, 2순위 생성일 오름차순
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate <= :today " +
      "ORDER BY ti.targetDate ASC, ti.createdAt ASC")
  List<TodoInstance> findRoomAllTodos(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today
  );

  // 상태가 특정값이 아닌 모든 건 조회
  List<TodoInstance> findAllByRoomIdAndStatusNot(Long roomId, TodoStatus status);

  /**
   * [특정 구성원의 모든 할 일 조회]
   * 특정 유저 + 상태가 미완료(PENDING)인 건들을
   * 마감일 빠른 순(지연 -> 오늘 -> 예정)으로 전체 조회
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.actualAssignee.id = :userId " +
      "AND ti.status = 'PENDING' " + // 완료된 건은 제외하고 해야 할 일만!
      "ORDER BY ti.targetDate ASC")
  List<TodoInstance> findMemberTodos(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId);

  /**
   * [내 놓친 할 일 카운트]
   * 본인 담당 + 오늘 이전 + 미완료 상태인 건수
   */
  @Query("SELECT COUNT(ti) FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.targetDate < :today AND ti.status = :status")
  int countMissedTodos(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status);

  // 배치/로직용 유틸리티 메서드
  boolean existsByTodoIdAndTargetDate(Long todoId, LocalDate targetDate);
  long countByTargetDateBeforeAndStatus(LocalDate date, TodoStatus status);
  boolean existsByTodoAndTargetDate(Todo todo, LocalDate date);

  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId AND ti.status = :status ORDER BY ti.createdAt DESC")
  Slice<TodoInstance> findCompletedTodos(@Param("roomId") Long roomId, @Param("status") TodoStatus status, Pageable pageable);

  List<TodoInstance> findAllByTodoIdAndTargetDateAfter(Long todoId, LocalDate today);
}