package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

public interface TodoService {
  // 할 일 등록
  TodoCreateResponse createTodo(User user, TodoCreateRequest request);

  // 할 일 수정
  TodoCreateResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request);

  // 나의 할 일 전체 조회
  MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor);

  // 나의 할 일 - 지연된 할 일
  MyTodoListResponse.PagedTodoResponse getMissedTodos(Long userId, String cursor);

  // 나의 할 일 - 오늘 할 일
  MyTodoListResponse.PagedTodoResponse getTodayTodos(Long userId, String cursor);

  // 나의 할 일 - 예정된 할 일
  MyTodoListResponse.PagedTodoResponse getUpcomingTodos(Long userId, String cursor);

  // 우리집 할 일 - 오늘 할 일
  RoomTodoResponse getRoomTodoList(Long userId);

  // 우리집 할 일 - 지연된 할 일
  List<TodoSummaryResponse> getRoomDelayedTodo(Long userId);

  // 우리집 할 일 - 전체 조회 (오늘+지연+예정)
  List<TodoSummaryResponse> getRoomAllTodo(Long userId);

  // 특정 구성원의 할 일 조회
  List<TodoSummaryResponse> getMemberTodo(Long loginUserId, Long targetMemberId);

  // 내 놓친 할 일 카운트
  int getMissedTodoCount(Long userId);

  // 완료된 할 일
  Slice<TodoCompletedResponse> getCompletedTodos(Long userId, int page, int size);

  // 할 일 완료 처리
  void completeTodo(Long userId, Long instanceId, MultipartFile image);

  // 할 일 인증샷 삭제
  void deleteTodoImage(Long userId, Long instanceId);
}