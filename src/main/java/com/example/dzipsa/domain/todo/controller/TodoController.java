package com.example.dzipsa.domain.todo.controller;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.service.TodoBatchService;
import com.example.dzipsa.domain.todo.service.TodoService;
import com.example.dzipsa.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

  private final TodoService todoService;
  private final TodoBatchService todoBatchService;

  /**
   * [할 일 등록]
   * URL: POST /api/todos
   * @param request 제목, 메모, 반복 설정 등 상세 정보
   */
  @PostMapping
  public ResponseEntity<TodoCreateResponse> createTodo(
      @AuthenticationPrincipal User user,
      @RequestBody TodoCreateRequest request) {
    TodoCreateResponse response = todoService.createTodo(user, request);
    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 수정]
   * URL: PUT /api/todos/{todoId}
   * 마스터 정보를 수정하며, 오늘 이후 예정된 미완료 인스턴스들에 변경사항을 반영
   */
  @PutMapping("/{todoId}")
  public ResponseEntity<TodoCreateResponse> updateTodo(
      @AuthenticationPrincipal User user,
      @PathVariable Long todoId,
      @RequestBody TodoUpdateRequest request) {
    return ResponseEntity.ok(todoService.updateTodo(user.getId(), todoId, request));
  }

  /**
   * [나의 할 일 - 전체 섹션 통합 조회]
   * URL: GET /api/todos/me/all
   * 최초 진입 시 지연(Missed)/오늘(Today)/예정(Upcoming) 데이터를 한 번에 가져옴
   */
  @GetMapping("/me/all")
  public ResponseEntity<MyTodoListResponse> getMyTodoList(
      @AuthenticationPrincipal User user,
      @RequestParam(required = false) String missedCursor,
      @RequestParam(required = false) String todayCursor,
      @RequestParam(required = false) String upcomingCursor) {
    MyTodoListResponse response = todoService.getMyTodoList(user.getId(), missedCursor, todayCursor,
        upcomingCursor);
    return ResponseEntity.ok(response);
  }

  /**
   * [나의 할 일 - 놓친 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/missed
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-17_42)
   */
  @GetMapping("/me/missed")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getMissedTodos(
      @AuthenticationPrincipal User user,
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getMissedTodos(user.getId(), cursor));
  }

  /**
   * [나의 할 일 - 오늘 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/today
   * @param cursor 이전 페이지의 마지막 ID (ex. 42)
   */
  @GetMapping("/me/today")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getTodayTodos(
      @AuthenticationPrincipal User user,
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getTodayTodos(user.getId(), cursor));
  }

  /**
   * [나의 할 일 - 예정된 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/upcoming
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-19_42)
   */
  @GetMapping("/me/upcoming")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getUpcomingTodos(
      @AuthenticationPrincipal User user,
      @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getUpcomingTodos(user.getId(), cursor));
  }

  /**
   * [우리집 할 일 - 전체 현황 조회]
   * URL: GET /api/todos/room/all
   * 우리 집 멤버들의 오늘을 포함한 과거의 모든 할 일 히스토리를 최신순으로 조회
   */
  @GetMapping("/room/all")
  public ResponseEntity<List<TodoSummaryResponse>> getRoomAllTodo(
      @AuthenticationPrincipal User user) {
    List<TodoSummaryResponse> response = todoService.getRoomAllTodo(user.getId());
    return ResponseEntity.ok(response);
  }

  /**
   * [우리집 할 일 - 오늘 할 일 조회]
   * URL: GET /api/todos/room/today
   * 방 멤버 전체의 오늘 할 일 목록을 생성순으로 조회
   */
  @GetMapping("/room/today")
  public ResponseEntity<List<TodoSummaryResponse>> getRoomTodayTodo(
      @AuthenticationPrincipal User user) {
    List<TodoSummaryResponse> response = todoService.getRoomTodoList(user.getId());
    return ResponseEntity.ok(response);
  }

  /**
   * [우리집 할 일 - 지연된 할 일 조회]
   * URL: GET /api/todos/room/delayed
   */
  @GetMapping("/room/delayed")
  public ResponseEntity<List<TodoSummaryResponse>> getRoomDelayedTodo(
      @AuthenticationPrincipal User user) {
    // 기존 build()에서 서비스 호출로 변경
    List<TodoSummaryResponse> response = todoService.getRoomDelayedTodo(user.getId());
    return ResponseEntity.ok(response);
  }

  /**
   * [우리집 할 일 - 특정 구성원 할 일 조회]
   * URL: GET /api/todos/room/members/{memberId}
   * 해당 구성원의 지연된 일 + 오늘 할 일 + 예정된 할 일을 모두 조회
   */
  @GetMapping("/room/members/{memberId}")
  public ResponseEntity<List<TodoSummaryResponse>> getMemberTodo(
      @AuthenticationPrincipal User user,
      @PathVariable Long memberId) {

    List<TodoSummaryResponse> response = todoService.getMemberTodo(user.getId(), memberId);

    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 인증샷 삭제]
   * URL: DELETE /api/todos/instances/{instanceId}/image
   * 사용자가 '-' 버튼을 눌렀을 때 호출되어 S3 파일과 DB URL을 삭제함
   */
  @DeleteMapping("/instances/{instanceId}/image")
  public ResponseEntity<Void> deleteTodoImage(
      @AuthenticationPrincipal User user,
      @PathVariable Long instanceId) {

    todoService.deleteTodoImage(user.getId(), instanceId);
    return ResponseEntity.ok().build();
  }

  /**
   * [완료된 할 일 리스트 조회]
   * URL: GET /api/todos/completed
   * 특정 방에서 인증샷과 함께 완료된 할 일들을 최신 완료순으로 조회
   */
  @GetMapping("/completed")
  public ResponseEntity<Slice<TodoCompletedResponse>> getCompletedTodos(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Slice<TodoCompletedResponse> response = todoService.getCompletedTodos(user.getId(), page, size);
    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 완료 처리 - 인증샷 포함]
   * URL: PATCH /api/todos/instances/{instanceId}/complete
   * S3 이미지 업로드 후 반환된 URL을 저장
   */
  @PatchMapping(value = "/instances/{instanceId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> completeTodo(
      @AuthenticationPrincipal User user,
      @PathVariable Long instanceId,
      @RequestPart(value = "image", required = false) MultipartFile image // 선택 사항으로 변경
  ) {
    todoService.completeTodo(user.getId(), instanceId, image);
    return ResponseEntity.ok().build();
  }
}