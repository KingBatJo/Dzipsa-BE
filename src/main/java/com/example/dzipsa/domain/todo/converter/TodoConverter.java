package com.example.dzipsa.domain.todo.converter;

import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoDetailResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.user.entity.User;
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

  // 완료된 할 일 DTO 변환 (인증샷 포함 리스트용)
  public static TodoCompletedResponse toCompletedDTO(TodoInstance instance) {
    User assignee = instance.getActualAssignee();
    return TodoCompletedResponse.builder()
        .instanceId(instance.getId())
        .title(instance.getTodo().getTitle())
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee != null ? assignee.getProfileImageUrl() : null)
        .imageUrl(instance.getImageUrl())
        .completedAt(instance.getCompletedAt() != null
            ? instance.getCompletedAt().format(DateTimeFormatter.ofPattern("a hh:mm"))
            : null)
        .build();
  }

  // 단일 인스턴스 요약 정보 변환 (지연/오늘/예정 공통)
  public static TodoSummaryResponse toSummaryResponse(TodoInstance instance) {
    User assignee = instance.getActualAssignee();

    return TodoSummaryResponse.builder()
        .instanceId(instance.getId())
        .title(instance.getTodo().getTitle())
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee != null ? assignee.getProfileImageUrl() : null)
        .status(instance.getStatus())
        .targetDate(instance.getTargetDate())
        .delayDays(calculateDelay(instance.getTargetDate(), instance.getStatus()))
        .imageUrl(instance.getImageUrl())
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

  // 지연 날짜 계산
  private static Long calculateDelay(LocalDate targetDate, TodoStatus status) {
    if (status == TodoStatus.COMPLETED) return 0L;
    long days = ChronoUnit.DAYS.between(targetDate, LocalDate.now());
    return days > 0 ? days : 0L;
  }

  // 커서 생성 로직 (날짜_ID 결합)
  private static String generateCursor(Slice<TodoInstance> slice) {
    if (!slice.hasContent()) return null;
    TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);
    return lastItem.getTargetDate().toString() + "_" + lastItem.getId();
  }

  public static TodoCreateResponse toCreateResponse(Todo todo, TodoInstance instance) {
    User assignee = (instance != null) ? instance.getActualAssignee() : todo.getDefaultAssignee();
    return TodoCreateResponse.builder()
        .todoId(todo.getId())
        .instanceId(instance != null ? instance.getId() : null)
        .title(todo.getTitle())
        .memo(todo.getMemo())
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
    LocalDate today = LocalDate.now();

    // 날짜 포맷 정의 (예: 2026. 3. 23 (월))
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. M. d (E)");

    // 상태 및 세부 문구 계산 기본값
    String statusStr = "진행";
    String statusDetail = instance.getTargetDate().format(formatter);

    if (instance.getStatus() == TodoStatus.COMPLETED) {
      statusStr = "완료";

      // 실제 완료된 날짜 포맷팅
      String completedDateStr = (instance.getCompletedAt() != null)
          ? instance.getCompletedAt().format(formatter)
          : statusDetail;

      if (instance.getCompletedAt() != null && instance.getCompletedAt().toLocalDate().isAfter(instance.getTargetDate())) {
        statusStr = "지연완료";
        long delayedDays = ChronoUnit.DAYS.between(instance.getTargetDate(), instance.getCompletedAt().toLocalDate());
        // "n일 지연, 완료날짜"
        statusDetail = delayedDays + "일 지연, " + completedDateStr;
      } else {
        // 제때 완료한 경우 완료된 날짜 표시
        statusDetail = completedDateStr;
      }
    } else if (instance.getTargetDate().isBefore(today)) {
      statusStr = "지연";
      long delayedDays = ChronoUnit.DAYS.between(instance.getTargetDate(), today);
      statusDetail = delayedDays + "일 지연";
    }

    return TodoDetailResponse.builder()
        .instanceId(instance.getId())
        .title(todo.getTitle())
        .targetDate(instance.getTargetDate())
        .memo(todo.getMemo())
        .assigneeId(assignee != null ? assignee.getId() : null)
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .assigneeProfileImage(assignee != null ? assignee.getProfileImageUrl() : null)
        .recurringInfo(formatRecurringText(todo.getRecurringType(), todo.getRepeatDays()))
        .status(statusStr)
        .statusDetail(statusDetail)
        .imageUrl(instance.getImageUrl())
        .isOwner(assignee != null && assignee.getId().equals(userId))
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
}