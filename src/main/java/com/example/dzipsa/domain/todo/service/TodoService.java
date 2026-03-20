package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

public interface TodoService {
  void createTodo(Long userId, Long roomId, TodoCreateRequest request);

  void deleteTodoImage(Long userId, Long instanceId);

  MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor);
  MyTodoListResponse.PagedTodoResponse getMissedTodos(Long userId, String cursor);
  MyTodoListResponse.PagedTodoResponse getTodayTodos(Long userId, String cursor);
  MyTodoListResponse.PagedTodoResponse getUpcomingTodos(Long userId, String cursor);

  // 우리집 오늘 전체 할 일 (기존)
  List<TodoSummaryResponse> getRoomTodoList(Long roomId);

  // 우리집 지연된 할 일 전체 조회
  List<TodoSummaryResponse> getRoomDelayedTodo(Long roomId);

  // 우리집 모든 할 일 전체 조회 (오늘+지연 포함 등)
  List<TodoSummaryResponse> getRoomAllTodo(Long roomId);

  // 특정 구성원의 할 일 조회
  List<TodoSummaryResponse> getMemberTodo(Long roomId, Long userId);

  // 내 놓친 할 일 카운트
  int getMissedTodoCount(Long userId);

  Slice<TodoCompletedResponse> getCompletedTodos(Long roomId, int page, int size);
  void completeTodo(Long userId, Long instanceId, MultipartFile image);
  void updateTodo(Long userId, Long todoId, TodoUpdateRequest request);
  void generateRecurringTodos();
}