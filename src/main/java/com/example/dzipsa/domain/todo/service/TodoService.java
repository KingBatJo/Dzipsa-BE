package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.*;
import com.example.dzipsa.domain.user.entity.User;
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

  /**
   * [우리 집 할 일 - 상단 넛지 및 섹션별 숫자 통계]
   * ServiceImpl의 @Override 에러 해결을 위해 추가
   */
  TodoNudgeResponse getRoomTodoStats(Long userId);

  /**
   * [우리 집 할 일 - 오늘 할 일 리스트만 조회]
   * 기존 RoomTodoResponse 대신 PagedTodoResponse를 반환하도록 수정
   */
  MyTodoListResponse.PagedTodoResponse getRoomTodayTodoList(Long userId, String cursor);

  // 우리집 할 일 - 지연된 할 일
  MyTodoListResponse.PagedTodoResponse getRoomDelayedTodo(Long userId, String cursor);

  // 우리집 할 일 - 전체 조회 (오늘+지연+예정)
  MyTodoListResponse.PagedTodoResponse getRoomAllTodo(Long userId, String cursor);

  // 특정 구성원의 할 일 조회
  MyTodoListResponse.PagedTodoResponse getMemberTodo(Long loginUserId, Long targetMemberId, String cursor);

  // 내 놓친 할 일 카운트
  int getMissedTodoCount(Long userId);

  // 완료된 할 일 목록 조회
  MyTodoListResponse.PagedTodoResponse getCompletedTodos(Long userId, String cursor);

  // 할 일 완료 처리
  void completeTodo(Long userId, Long instanceId, MultipartFile image);

  // 할 일 인증샷 삭제
  void deleteTodoImage(Long userId, Long instanceId);

  // 할 일 상세 조회
  TodoDetailResponse getTodoDetail(Long userId, Long instanceId);

  // 할 일 상태 되돌리기 (완료 -> 진행 중)
  void resetTodoStatus(Long userId, Long instanceId);
}