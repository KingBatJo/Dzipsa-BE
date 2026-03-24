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
 */
public class TodoConverter {

  /**
   * 단일 할 일 요약 정보 변환
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
        .targetDate(instance.getTargetDate())
        .delayDays(calculateDelay(instance))
        .imageUrl(instance.getImageUrl())
        .completedAt(instance.getCompletedAt() != null
            ? instance.getCompletedAt().toString()
            : null)
        .recurringType(instance.getTodo().getRecurringType())
        .repeatDays(instance.getTodo().getRepeatDays())
        .build();
  }

  /**
   * Slice 데이터를 페이징 응답 객체로 변환
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
    // [수정] 표준 LocalDate 사용으로 원복
    LocalDate today = LocalDate.now();

    if (instance.getStatus() == TodoStatus.COMPLETED) {
      if (instance.getCompletedAt() == null) return 0L;
      long days = ChronoUnit.DAYS.between(targetDate, instance.getCompletedAt().toLocalDate());
      return Math.max(0, days);
    }

    long days = ChronoUnit.DAYS.between(targetDate, today);
    return Math.max(0, days);
  }

  // 커서 생성 로직
  private static String generateCursor(Slice<TodoInstance> slice) {
    if (!slice.hasContent()) return null;
    TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);
    // [수정] 표준 LocalDate 사용으로 원복
    LocalDate today = LocalDate.now();

    if (lastItem.getStatus() == TodoStatus.COMPLETED && lastItem.getCompletedAt() != null) {
      return lastItem.getCompletedAt().toString() + "_" + lastItem.getId();
    }

    if (lastItem.getTargetDate().equals(today)) {
      return lastItem.getId().toString();
    }

    return lastItem.getTargetDate().toString() + "_" + lastItem.getId();
  }

  public static TodoCreateResponse toCreateResponse(Todo todo, TodoInstance instance, LocalDate targetDate) {
    User assignee = (instance != null) ? instance.getActualAssignee() : todo.getDefaultAssignee();

    return TodoCreateResponse.builder()
        .todoId(todo.getId())
        .instanceId(instance != null ? instance.getId() : null)
        .title(todo.getTitle())
        .memo(todo.getMemo())
        .targetDate(targetDate)
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
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee != null ? assignee.getProfileImageUrl() : null)
        .targetDate(instance.getTargetDate())
        .recurringType(todo.getRecurringType())
        .repeatDays(todo.getRepeatDays())
        .startDate(todo.getStartDate())
        .endDate(todo.getEndDate())
        .isRandom(todo.getIsRandom())
        .status(instance.getStatus())
        .completedAt(instance.getCompletedAt())
        .delayDays(calculateDelay(instance))
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