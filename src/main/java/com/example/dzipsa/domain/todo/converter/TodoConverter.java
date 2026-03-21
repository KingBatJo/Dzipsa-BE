package com.example.dzipsa.domain.todo.converter;

import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.user.entity.User;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * [Todo 데이터 변환기]
 * 엔티티를 클라이언트 응답용 DTO로 변환
 * 세 가지 리스트(지연/오늘/예정)의 개별 무한 스크롤을 위한 페이징 변환 로직을 포함
 */
public class TodoConverter {

  // 완료된 할 일 DTO 변환 (인증샷 포함 리스트용)
  public static TodoCompletedResponse toCompletedDTO(TodoInstance instance) {
    return TodoCompletedResponse.builder()
        .instanceId(instance.getId())
        .title(instance.getTodo().getTitle())
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
        .assigneeNickname(assignee != null ? assignee.getNickname() : "미지정")
        .profileImageUrl(assignee.getProfileImageUrl())
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
    return TodoCreateResponse.builder()
        .todoId(todo.getId())
        .instanceId(instance != null ? instance.getId() : null)
        .title(todo.getTitle())
        .memo(todo.getMemo())
        .defaultAssigneeId(todo.getDefaultAssignee() != null ? todo.getDefaultAssignee().getId() : null)
        .isRandom(todo.getIsRandom())
        .recurringType(todo.getRecurringType())
        .repeatDays(todo.getRepeatDays())
        .startDate(todo.getStartDate())
        .endDate(todo.getEndDate())
        .build();
  }
}