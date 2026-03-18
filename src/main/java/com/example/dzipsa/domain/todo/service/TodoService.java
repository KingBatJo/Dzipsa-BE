package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface TodoService {
  void createTodo(Long userId, Long roomId, TodoCreateRequest request);

  // 3중 커서를 지원하도록 파라미터 수정
  MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor);

  List<TodoSummaryResponse> getRoomTodoList(Long roomId);

  Slice<TodoCompletedResponse> getCompletedTodos(Long roomId, int page, int size);

  void completeTodo(Long userId, Long instanceId, String imageUrl);

  void updateTodo(Long userId, Long todoId, TodoUpdateRequest request);

  void generateRecurringTodos();
}