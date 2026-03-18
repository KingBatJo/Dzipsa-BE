package com.example.dzipsa.domain.todo.controller;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

  private final TodoService todoService;

  /**
   * [할 일 신규 등록]
   * URL: POST /api/todos
   * @param roomId 할 일이 속할 방 ID
   * @param request 제목, 메모, 반복 설정 등 상세 정보
   */
  @PostMapping
  public ResponseEntity<Void> createTodo(
      @RequestParam Long roomId,
      @RequestBody TodoCreateRequest request) {
    todoService.createTodo(1L, roomId, request);
    return ResponseEntity.ok().build();
  }

  /**
   * [나의 할 일 - 전체 섹션 통합 조회]
   * URL: GET /api/todos/me/all
   * 최초 진입 시 지연(Missed)/오늘(Today)/예정(Upcoming) 데이터를 한 번에 가져옴
   */
  @GetMapping("/me/all")
  public ResponseEntity<MyTodoListResponse> getMyTodoList(
      @RequestParam(required = false) String missedCursor,
      @RequestParam(required = false) String todayCursor,
      @RequestParam(required = false) String upcomingCursor) {
    MyTodoListResponse response = todoService.getMyTodoList(1L, missedCursor, todayCursor, upcomingCursor);
    return ResponseEntity.ok(response);
  }

  /**
   * [나의 할 일 - 놓친 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/missed
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-17_42)
   */
  @GetMapping("/me/missed")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getMissedTodos(
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getMissedTodos(1L, cursor));
  }

  /**
   * [나의 할 일 - 오늘 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/today
   * @param cursor 이전 페이지의 마지막 ID (ex. 42)
   */
  @GetMapping("/me/today")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getTodayTodos(
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getTodayTodos(1L, cursor));
  }

  /**
   * [나의 할 일 - 예정된 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/upcoming
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-19_42)
   */
  @GetMapping("/me/upcoming")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getUpcomingTodos(
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getUpcomingTodos(1L, cursor));
  }

  /**
   * [우리집 할 일 - 오늘 할 일 전체 현황 조회]
   * URL: GET /api/todos/room/today
   * 방 멤버 전체의 오늘 할 일 목록을 생성순으로 조회
   */
  @GetMapping("/room/today")
  public ResponseEntity<List<TodoSummaryResponse>> getRoomTodayTodo(
      @RequestParam Long roomId) {
    List<TodoSummaryResponse> response = todoService.getRoomTodoList(roomId);
    return ResponseEntity.ok(response);
  }

  /**
   * [우리집 할 일 - 지연된 할 일 전체 현황 조회]
   * URL: GET /api/todos/room/delayed
   * 방 멤버 전체의 지연된 할 일 목록을 조회
   */
  @GetMapping("/room/delayed")
  public ResponseEntity<List<TodoSummaryResponse>> getRoomDelayedTodo(
      @RequestParam Long roomId) {
    // TODO: 서비스 레이어에 지연 조회 로직 추가 필요
    return ResponseEntity.ok().build();
  }

  /**
   * [완료된 할 일 리스트 조회]
   * URL: GET /api/todos/completed
   * 특정 방에서 인증샷과 함께 완료된 할 일들을 최신 완료순으로 조회
   */
  @GetMapping("/completed")
  public ResponseEntity<Slice<TodoCompletedResponse>> getCompletedTodos(
      @RequestParam Long roomId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Slice<TodoCompletedResponse> response = todoService.getCompletedTodos(roomId, page, size);
    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 완료 처리 - 인증샷 포함]
   * URL: PATCH /api/todos/instances/{instanceId}/complete
   * S3 이미지 업로드 후 반환된 URL을 저장
   */
  @PatchMapping("/instances/{instanceId}/complete")
  public ResponseEntity<Void> completeTodo(
      @PathVariable Long instanceId,
      @RequestBody String imageUrl) {
    todoService.completeTodo(1L, instanceId, imageUrl);
    return ResponseEntity.ok().build();
  }

  /**
   * [할 일 원본 설정 수정 및 미래 담당자 전파]
   * URL: PUT /api/todos/{todoId}
   * 마스터 정보를 수정하며, 오늘 이후 예정된 미완료 인스턴스들에 변경 사항을 반영
   */
  @PutMapping("/{todoId}")
  public ResponseEntity<Void> updateTodo(
      @PathVariable Long todoId,
      @RequestBody TodoUpdateRequest request) {
    todoService.updateTodo(1L, todoId, request);
    return ResponseEntity.ok().build();
  }
}