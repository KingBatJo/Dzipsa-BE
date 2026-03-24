package com.example.dzipsa.domain.todo.repository;

import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodoInstanceRepository extends JpaRepository<TodoInstance, Long> {

  /**
   * [배치/검증용] 특정 Todo의 특정 날짜 인스턴스 존재 여부 확인
   */
  boolean existsByTodoIdAndTargetDate(Long todoId, LocalDate targetDate);

  /**
   * [배치용] 오늘 날짜 이전이면서 특정 상태인 인스턴스 개수 조회
   */
  long countByTargetDateBeforeAndStatus(LocalDate date, TodoStatus status);

  /**
   * [나의 할 일 - 지연된 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.status != 'COMPLETED' AND ti.targetDate < :today " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findMissedTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [나의 할 일 - 오늘의 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.targetDate = :today AND ti.status = :status " +
      "AND ti.id > :cursorId " +
      "ORDER BY ti.id ASC")
  Slice<TodoInstance> findTodayTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [나의 할 일 - 예정된 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.status = :status " +
      "AND (ti.targetDate > :today) " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findUpcomingTodosWithCursor(
      @Param("userId") Long userId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [우리집 할 일 - 오늘 할 일 - 무한 스크롤]
   * 상태(status) 필터를 추가하여 '완료'된 항목 등을 제외할 수 있음
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId AND ti.targetDate = :today " +
      "AND ti.status = :status AND ti.id > :cursorId " +
      "ORDER BY ti.id ASC")
  Slice<TodoInstance> findRoomTodayTodosWithCursor(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [우리집 할 일 - 지연된 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate < :today AND ti.status = :status " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findRoomDelayedTodosWithCursor(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [우리집 할 일 - 모든 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.status != :status " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findRoomAllTodosWithCursor(
      @Param("roomId") Long roomId,
      @Param("status") TodoStatus status,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [특정 구성원 할 일 - 무한 스크롤]
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.actualAssignee.id = :userId AND ti.status = 'PENDING' " +
      "AND (ti.targetDate > :cursorDate OR (ti.targetDate = :cursorDate AND ti.id > :cursorId)) " +
      "ORDER BY ti.targetDate ASC, ti.id ASC")
  Slice<TodoInstance> findMemberTodosWithCursor(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId,
      @Param("cursorDate") LocalDate cursorDate,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /**
   * [우리집 할 일 - 상단 넛지 통계용 (오늘 전체 리스트)]
   * 에러 해결: 서비스에서 호출하는 인자 2개 버전 유지 (오늘 전체 할 일 개수 계산용)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId AND ti.targetDate = :today")
  List<TodoInstance> findRoomTodayTodos(@Param("roomId") Long roomId, @Param("today") LocalDate today);

  /**
   * [우리집 할 일 - 상단 넛지 통계용 (상태 필터링 포함)]
   * 특정 상태의 오늘 할 일 리스트를 가져올 때 사용
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate = :today AND ti.status = :status")
  List<TodoInstance> findRoomTodayTodos(@Param("roomId") Long roomId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status);

  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate < :today AND ti.status = :status")
  List<TodoInstance> findRoomDelayedTodos(@Param("roomId") Long roomId, @Param("today") LocalDate today, @Param("status") TodoStatus status);

  @Query("SELECT COUNT(ti) FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId " +
      "AND ti.targetDate < :today AND ti.status = :status")
  int countMissedTodos(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("status") TodoStatus status);

  /**
   * [완료된 할 일 조회 - 무한 스크롤]
   * 정렬 기준: completedAt DESC (최신 완료순)
   * 커서: 완료일시_ID (LocalDateTime_Long)
   */
  @Query("SELECT ti FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.status = :status " +
      "AND (:cursorDateTime IS NULL OR " +
      "    (ti.completedAt < :cursorDateTime OR (ti.completedAt = :cursorDateTime AND ti.id < :cursorId))) " +
      "ORDER BY ti.completedAt DESC, ti.id DESC")
  Slice<TodoInstance> findCompletedTodosWithCursor(
      @Param("roomId") Long roomId,
      @Param("status") TodoStatus status,
      @Param("cursorDateTime") LocalDateTime cursorDateTime,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  Optional<TodoInstance> findByTodoIdAndTargetDate(Long todoId, LocalDate targetDate);

  @Modifying
  @Query("DELETE FROM TodoInstance ti WHERE ti.todo.id = :todoId AND ti.targetDate >= :now")
  void deleteFutureInstances(@Param("todoId") Long todoId, @Param("now") LocalDate now);

  List<TodoInstance> findAllByRoomIdAndStatusNot(Long roomId, TodoStatus status);
  List<TodoInstance> findAllByTodoIdAndTargetDateAfter(Long todoId, LocalDate today);

  /**
   * [우리집 할 일 - 지연된 할 일 개수 조회]
   * 오늘 이전이면서 완료되지 않은(PENDING/DELAYED 등) 할 일의 개수
   */
  @Query("SELECT COUNT(ti) FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.targetDate < :today AND ti.status = :status")
  long countRoomDelayedTodos(
      @Param("roomId") Long roomId,
      @Param("today") LocalDate today,
      @Param("status") TodoStatus status
  );

  /**
   * [우리집 할 일 - 모든 할 일 개수 조회]
   * 특정 상태(예: DELETED 등)를 제외한 모든 할 일의 개수
   */
  @Query("SELECT COUNT(ti) FROM TodoInstance ti WHERE ti.room.id = :roomId " +
      "AND ti.status != :status")
  long countRoomAllTodos(
      @Param("roomId") Long roomId,
      @Param("status") TodoStatus status
  );

  /**
   * [구성원 통계용] 특정 담당자의 모든 미완료 할 일 개수 (날짜 상관없음)
   */
  @Query("SELECT COUNT(ti) FROM TodoInstance ti WHERE ti.actualAssignee.id = :userId AND ti.status = :status")
  int countTotalPendingByMember(@Param("userId") Long userId, @Param("status") TodoStatus status);

  /**
   * [할 일 삭제 - SINCE_THIS]
   * 특정 날짜 이후의 미완료(PENDING) 인스턴스들을 물리 삭제
   */
  @Modifying
  @Query("DELETE FROM TodoInstance ti WHERE ti.todo.id = :todoId " +
      "AND ti.targetDate >= :targetDate AND ti.status = :status")
  void deleteAllByTodoIdAndTargetDateGreaterThanEqualAndStatus(
      @Param("todoId") Long todoId,
      @Param("targetDate") LocalDate targetDate,
      @Param("status") TodoStatus status
  );

  /**
   * [할 일 삭제 - ALL_RECURRING]
   * 해당 Todo의 모든 미완료(PENDING) 인스턴스들을 물리 삭제
   */
  @Modifying
  @Query("DELETE FROM TodoInstance ti WHERE ti.todo.id = :todoId AND ti.status = :status")
  void deleteAllByTodoIdAndStatus(
      @Param("todoId") Long todoId,
      @Param("status") TodoStatus status
  );
}