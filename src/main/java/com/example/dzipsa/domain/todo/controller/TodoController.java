package com.example.dzipsa.domain.todo.controller;

import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoDetailResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.service.TodoBatchService;
import com.example.dzipsa.domain.todo.service.TodoService;
import com.example.dzipsa.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@Tag(name = "Todo", description = "할 일(Todo) 관련 API")
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
  @Operation(summary = "할 일 신규 등록", description = "새로운 할 일을 등록하고 반복 설정 시 인스턴스를 생성합니다.")
  @PostMapping
  public ResponseEntity<TodoCreateResponse> createTodo(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody TodoCreateRequest request) {
    TodoCreateResponse response = todoService.createTodo(user, request);
    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 수정]
   * URL: PUT /api/todos/{todoId}
   * 마스터 정보를 수정하며, 오늘 이후 예정된 미완료 인스턴스들에 변경사항을 반영
   */
  @Operation(summary = "할 일 수정", description = "할 일 마스터 정보를 수정하고 향후 일정을 재정비합니다.")
  @PutMapping("/{todoId}")
  public ResponseEntity<TodoCreateResponse> updateTodo(
      @AuthenticationPrincipal User user,
      @Parameter(description = "수정할 할 일 마스터 ID") @PathVariable Long todoId,
      @Valid @RequestBody TodoUpdateRequest request) {
    return ResponseEntity.ok(todoService.updateTodo(user.getId(), todoId, request));
  }

  /**
   * [나의 할 일 - 전체 섹션 통합 조회]
   * URL: GET /api/todos/me/all
   * 최초 진입 시 지연(Missed)/오늘(Today)/예정(Upcoming) 데이터를 한 번에 가져옴
   */
  @Operation(summary = "나의 할 일 전체 조회", description = "지연/오늘/예정 섹션 데이터를 한 번에 조회합니다.")
  @GetMapping("/me/all")
  public ResponseEntity<MyTodoListResponse> getMyTodoList(
      @AuthenticationPrincipal User user,
      @Parameter(description = "지연된 할 일 커서 (날짜_ID)") @RequestParam(required = false) String missedCursor,
      @Parameter(description = "오늘의 할 일 커서 (ID)") @RequestParam(required = false) String todayCursor,
      @Parameter(description = "예정된 할 일 커서 (날짜_ID)") @RequestParam(required = false) String upcomingCursor) {
    MyTodoListResponse response = todoService.getMyTodoList(user.getId(), missedCursor, todayCursor,
        upcomingCursor);
    return ResponseEntity.ok(response);
  }

  /**
   * [나의 할 일 - 놓친 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/missed
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-17_42)
   */
  @Operation(summary = "나의 할 일 - 지연된 할 일 페이징 조회", description = "지연된 할 일 섹션만 개별적으로 페이징 조회합니다.")
  @GetMapping("/me/missed")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getMissedTodos(
      @AuthenticationPrincipal User user,
      @Parameter(description = "이전 페이지 마지막 데이터의 날짜_ID") @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getMissedTodos(user.getId(), cursor));
  }

  /**
   * [나의 할 일 - 오늘 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/today
   * @param cursor 이전 페이지의 마지막 ID (ex. 42)
   */
  @Operation(summary = "나의 할 일 - 오늘 할 일 페이징 조회", description = "오늘의 할 일 섹션만 개별적으로 페이징 조회합니다.")
  @GetMapping("/me/today")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getTodayTodos(
      @AuthenticationPrincipal User user,
      @Parameter(description = "이전 페이지 마지막 데이터의 ID") @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getTodayTodos(user.getId(), cursor));
  }

  /**
   * [나의 할 일 - 예정된 할 일 섹션 페이징 조회]
   * URL: GET /api/todos/me/upcoming
   * @param cursor 이전 페이지의 마지막 날짜_ID (ex. 2024-03-19_42)
   */
  @Operation(summary = "나의 할 일 - 예정된 할 일 페이징 조회", description = "예정된 할 일 섹션만 개별적으로 페이징 조회합니다.")
  @GetMapping("/me/upcoming")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getUpcomingTodos(
      @AuthenticationPrincipal User user,
      @Parameter(description = "이전 페이지 마지막 데이터의 날짜_ID") @RequestParam(required = false) String cursor) {
    return ResponseEntity.ok(todoService.getUpcomingTodos(user.getId(), cursor));
  }

  /**
   * [우리집 할 일 - 전체 현황 조회]
   * URL: GET /api/todos/room/all
   * 우리 집 멤버들의 오늘을 포함한 과거의 모든 할 일 히스토리를 최신순으로 조회
   */
  @Operation(summary = "우리집 할 일 - 전체 미완료 현황 조회", description = "방 안의 모든 미완료 할 일을 날짜순으로 조회합니다.")
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
  @Operation(summary = "우리집 할 일 - 오늘 할 일 및 넛지 조회", description = "우리 집 구성원의 오늘 할 일과 요약(진행률) 정보를 조회합니다.")
  @GetMapping("/room/today")
  public ResponseEntity<RoomTodoResponse> getRoomTodoList(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(todoService.getRoomTodoList(user.getId()));
  }

  /**
   * [우리집 할 일 - 지연된 할 일 조회]
   * URL: GET /api/todos/room/delayed
   */
  @Operation(summary = "우리집 할 일 - 지연된 할 일 조회", description = "우리 집 구성원이 놓친 할 일들을 조회합니다.")
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
  @Operation(summary = "우리집 할 일 - 특정 구성원 할 일 조회", description = "선택한 구성원에게 할당된 모든 할 일 목록을 조회합니다.")
  @GetMapping("/room/members/{memberId}")
  public ResponseEntity<List<TodoSummaryResponse>> getMemberTodo(
      @AuthenticationPrincipal User user,
      @Parameter(description = "조회할 구성원 ID") @PathVariable Long memberId) {

    List<TodoSummaryResponse> response = todoService.getMemberTodo(user.getId(), memberId);

    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 인증샷 삭제]
   * URL: DELETE /api/todos/instances/{instanceId}/image
   * 사용자가 '-' 버튼을 눌렀을 때 호출되어 S3 파일과 DB URL을 삭제함
   */
  @Operation(summary = "할 일 인증샷 삭제", description = "등록된 할 일 인증샷 이미지를 삭제하고 상태를 업데이트합니다.")
  @DeleteMapping("/instances/{instanceId}/image")
  public ResponseEntity<Void> deleteTodoImage(
      @AuthenticationPrincipal User user,
      @Parameter(description = "이미지를 삭제할 할 일 인스턴스 ID") @PathVariable Long instanceId) {

    todoService.deleteTodoImage(user.getId(), instanceId);
    return ResponseEntity.ok().build();
  }

  /**
   * [완료된 할 일 리스트 조회 - 무한 스크롤]
   * URL: GET /api/todos/completed
   * * @param cursor 이전 페이지 마지막 데이터의 "완료일시_ID" (첫 요청 시 생략)
   * @param size   불러올 데이터 개수 (기본 10개)
   */
  @Operation(summary = "완료된 할 일 리스트 조회", description = "우리 집에서 완료된 모든 할 일을 최신 완료순으로 조회합니다.")
  @GetMapping("/completed")
  public ResponseEntity<MyTodoListResponse.PagedTodoResponse> getCompletedTodos(
      @AuthenticationPrincipal User user,
      @Parameter(description = "커서 (완료일시_ID)") @RequestParam(required = false) String cursor,
      @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

    // 서비스 호출 후 200 OK와 함께 페이징 데이터 반환
    return ResponseEntity.ok(todoService.getCompletedTodos(user.getId(), cursor, size));
  }

  /**
   * [할 일 완료 처리 - 인증샷 포함]
   * URL: PATCH /api/todos/instances/{instanceId}/complete
   * S3 이미지 업로드 후 반환된 URL을 저장
   */
  @Operation(summary = "할 일 완료 처리", description = "인증샷을 업로드하고 할 일을 완료 상태로 변경합니다.")
  @PatchMapping(value = "/instances/{instanceId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> completeTodo(
      @AuthenticationPrincipal User user,
      @Parameter(description = "완료 처리할 할 일 인스턴스 ID") @PathVariable Long instanceId,
      @Parameter(description = "인증샷 이미지 파일") @RequestPart(value = "image", required = false) MultipartFile image // 선택 사항으로 변경
  ) {
    todoService.completeTodo(user.getId(), instanceId, image);
    return ResponseEntity.ok().build();
  }

  /**
   * [할 일 상세 조회]
   */
  @Operation(summary = "할 일 상세 조회", description = "특정 할 일의 상세 정보(반복, 상태, 권한 등)를 조회합니다.")
  @GetMapping("/instances/{instanceId}")
  public ResponseEntity<TodoDetailResponse> getTodoDetail(
      @AuthenticationPrincipal User user,
      @Parameter(description = "조회할 할 일 인스턴스 ID") @PathVariable Long instanceId) {

    // 서비스 호출 시 현재 로그인한 사용자의 ID를 함께 넘겨 '본인 여부(isOwner)'를 판단함
    TodoDetailResponse response = todoService.getTodoDetail(user.getId(), instanceId);
    return ResponseEntity.ok(response);
  }

  /**
   * [할 일 상태 초기화: 완료 -> 진행 중으로 변경]
   */
  @Operation(summary = "할 일 상태 초기화", description = "완료된 할 일을 다시 '진행 중' 상태로 되돌립니다.")
  @PatchMapping("/instances/{instanceId}/reset")
  public ResponseEntity<Void> resetTodoStatus(
      @AuthenticationPrincipal User user,
      @Parameter(description = "초기화할 할 일 인스턴스 ID") @PathVariable Long instanceId) {

    // 본인의 할 일만 상태를 되돌릴 수 있도록 처리함
    todoService.resetTodoStatus(user.getId(), instanceId);
    return ResponseEntity.ok().build();
  }
}