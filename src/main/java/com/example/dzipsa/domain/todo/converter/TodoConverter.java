package com.example.dzipsa.domain.todo.converter;

import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse;
import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse.MemberTodoStatsResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoDetailResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoNudgeResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * [Todo 데이터 변환기]
 * 엔티티를 클라이언트 응답용 DTO로 변환
 * 세 가지 리스트(지연/오늘/예정)의 개별 무한 스크롤을 위한 페이징 변환 로직을 포함
 */
public class TodoConverter {

  /**
   * 단일 할 일 요약 정보 변환 (지연/오늘/예정/완료 히스토리 공통)
   * 통합된 TodoSummaryResponse 규격을 사용하여 모든 목록 API에 대응
   */
  public static TodoSummaryResponse toSummaryResponse(TodoInstance instance) {
    User assignee = instance.getActualAssignee();

    return TodoSummaryResponse.builder()
        .instanceId(instance.getId())
        .title(instance.getTodo().getTitle())
        .memo(instance.getTodo().getMemo())
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee != null ? assignee.getProfileImageUrl() : null)
        .status(instance.getStatus())
        .targetDate(instance.getTargetDate()) // 수행 예정일
        .delayDays(calculateDelay(instance)) // 지연 일수 계산
        .imageUrl(instance.getImageUrl()) // 완료 인증샷 URL
        .completedAt(instance.getCompletedAt() != null
            ? instance.getCompletedAt().toString() // 커서와 통일성을 위해 ISO 8601 원본 문자열 반환 (나노초 포함)
            : null)
        .build();
  }

  /**
   * Slice 데이터를 페이징 응답 객체로 변환 (다중 무한 스크롤용)
   */
  public static MyTodoListResponse.PagedTodoResponse toPagedResponse(Slice<TodoInstance> slice) {
    return MyTodoListResponse.PagedTodoResponse.builder()
        .content(slice.getContent().stream()
            .map(TodoConverter::toSummaryResponse)
            .collect(Collectors.toList()))
        .hasNext(slice.hasNext())
        .nextCursor(generateCursor(slice))
        .build();
  }

  // 지연 날짜 계산 로직
  private static Long calculateDelay(TodoInstance instance) {
    LocalDate targetDate = instance.getTargetDate();

    // 1. 완료된 경우: 완료일과 마감일 비교
    if (instance.getStatus() == TodoStatus.COMPLETED) {
      if (instance.getCompletedAt() == null) return 0L;

      long days = ChronoUnit.DAYS.between(targetDate, instance.getCompletedAt().toLocalDate());
      return Math.max(0, days); // 일찍 했으면(음수) 0, 늦게 했으면(양수) 그 숫자만큼 반환
    }

    // 2. 미완료인 경우: 오늘 날짜와 마감일 비교
    long days = ChronoUnit.DAYS.between(targetDate, LocalDate.now());
    return Math.max(0, days); // 아직 마감 전이면(음수) 0 반환
  }

  // 커서 생성 로직
  private static String generateCursor(Slice<TodoInstance> slice) {
    if (!slice.hasContent()) return null;
    TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);

    // 1. 완료된 할 일 섹션 (완료일시_ID) - 정밀도 유지를 위해 toString() 사용
    if (lastItem.getStatus() == TodoStatus.COMPLETED && lastItem.getCompletedAt() != null) {
      return lastItem.getCompletedAt().toString() + "_" + lastItem.getId();
    }

    // 2. 오늘 할 일 섹션 (ID만 반환)
    if (lastItem.getTargetDate().equals(LocalDate.now())) {
      return lastItem.getId().toString();
    }

    // 3. 지연/예정된 할 일 섹션 (예정일_ID)
    return lastItem.getTargetDate().toString() + "_" + lastItem.getId();
  }

  // 서비스에서 계산된 targetDate를 직접 받도록 파라미터 추가
  public static TodoCreateResponse toCreateResponse(Todo todo, TodoInstance instance, LocalDate targetDate) {
    // 1. 담당자 결정: 인스턴스가 있으면 인스턴스의 담당자, 없으면 기본 담당자
    User assignee = (instance != null) ? instance.getActualAssignee() : todo.getDefaultAssignee();

    return TodoCreateResponse.builder()
        .todoId(todo.getId())
        .instanceId(instance != null ? instance.getId() : null) // 인스턴스 없으면 null
        .title(todo.getTitle())
        .memo(todo.getMemo())
        .targetDate(targetDate) // 서비스에서 결정된 날짜를 그대로 전달
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .isRandom(todo.getIsRandom())
        .recurringType(todo.getRecurringType())
        .repeatDays(todo.getRepeatDays())
        .startDate(todo.getStartDate())
        .endDate(todo.getEndDate())
        .build();
  }

  /**
   * 할 일 상세 정보 응답 변환
   */
  public static TodoDetailResponse toDetailResponse(TodoInstance instance, Long userId) {
    User assignee = instance.getActualAssignee();
    Todo todo = instance.getTodo();

    return TodoDetailResponse.builder()
        .todoId(todo.getId())
        .instanceId(instance.getId())
        .title(todo.getTitle())
        .memo(todo.getMemo())
        // 담당자 정보
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee != null ? assignee.getProfileImageUrl() : null)
        // 반복 및 등록 정보 (프론트 요구: 등록 필드 다 넣기)
        .targetDate(instance.getTargetDate())
        .recurringType(todo.getRecurringType())
        .repeatDays(todo.getRepeatDays())
        .startDate(todo.getStartDate())
        .endDate(todo.getEndDate())
        .isRandom(todo.getIsRandom())
        // 상태 및 시간 데이터
        .status(instance.getStatus())
        .completedAt(instance.getCompletedAt())
        .delayDays(calculateDelay(instance))
        // 인증샷 및 권한
        .imageUrl(instance.getImageUrl())
        .isOwner(assignee != null && assignee.getId().equals(userId))
        .isWriter(todo.getWriter().getId().equals(userId))
        .build();
  }

  private static String formatRecurringText(RecurringType type, String days) {
    if (type == RecurringType.NONE) return "반복 없음";

    if (type == RecurringType.WEEKLY && days != null) {
      String convertedDays = Arrays.stream(days.split(","))
          .map(String::trim)
          .map(TodoConverter::dayNumberToKorean)
          .collect(Collectors.joining(", "));
      return "매주 " + convertedDays;
    }

    if (type == RecurringType.MONTHLY && days != null) {
      return "매월 " + days + "일";
    }

    return "반복 설정 오류";
  }

  // 1(월) ~ 7(일) 기준 요일 변환
  private static String dayNumberToKorean(String dayNum) {
    return switch (dayNum) {
      case "1" -> "월";
      case "2" -> "화";
      case "3" -> "수";
      case "4" -> "목";
      case "5" -> "금";
      case "6" -> "토";
      case "7" -> "일";
      default -> "";
    };
  }

  /**
   * 우리집 할 일 메인 현황(통계) 변환
   */
  public static RoomTodoResponse toRoomTodoResponse(
      int totalToday, int completedToday, int myRemaining,
      int todayTotal, int delayedTotal, int allTotal,
      List<MemberTodoStatsResponse> memberStats,
      Slice<TodoInstance> todoSlice) {

    return RoomTodoResponse.builder()
        .nudgeInfo(TodoNudgeResponse.builder()
            .totalRoomTodoCount(totalToday)
            .completedRoomTodoCount(completedToday)
            .myRemainingTodoCount(myRemaining)
            .todayTotalCount(todayTotal)
            .delayedTotalCount(delayedTotal)
            .allTotalCount(allTotal)
            .build())
        .memberStats(memberStats)
        .todos(toPagedResponse(todoSlice))
        .build();
  }
}