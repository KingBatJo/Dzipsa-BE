package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.todo.converter.TodoConverter;
import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.RoomTodoResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCreateResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoNudgeResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.todo.repository.TodoInstanceRepository;
import com.example.dzipsa.domain.todo.repository.TodoRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.RoomErrorCode;
import com.example.dzipsa.global.util.S3Uploader;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoServiceImpl implements TodoService {

  private final TodoRepository todoRepository;
  private final TodoInstanceRepository todoInstanceRepository;
  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final S3Uploader s3Uploader;
  private final RoomMemberRepository roomMemberRepository;
  private final TodoBatchService todoBatchService; // 1. 배치 서비스 주입 추가

  /**
   * [할 일 신규 등록]
   * 1. 할 일의 메인 설정(마스터)을 저장
   * 2. 반복 설정이 있는 경우 즉시 14일치 인스턴스를 생성
   */
  @Override
  @Transactional
  public TodoCreateResponse createTodo(User user, TodoCreateRequest request) {
    RoomMember myMember = getActiveRoomMember(user.getId());
    Room myRoom = findRoomById(myMember.getRoomId());

    // 1. 담당자 결정: 프론트가 준 ID가 있으면 사용, 없으면 본인
    User targetAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId())
        .orElseThrow(() -> new EntityNotFoundException("지정된 담당자를 찾을 수 없습니다."))
        : user;

    // 2. Todo 마스터 저장 (앞으로의 기본값)
    Todo todo = Todo.builder()
        .room(myRoom)
        .writer(user)
        .defaultAssignee(targetAssignee)
        .title(request.getTitle())
        .memo(request.getMemo())
        .isRandom(request.getIsRandom())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .recurringType(request.getRecurringType())
        .repeatDays(request.getRepeatDays())
        .isActive(true)
        .build();
    todoRepository.save(todo);

    // 3. 즉시 인스턴스 생성 로직 (반복 설정에 따라 14일치 미리 생성)
    if (todo.getRecurringType() == RecurringType.NONE) {
      saveInstance(todo, myRoom, targetAssignee, todo.getStartDate());
    } else {
      // 반복 설정이 있다면 시작일로부터 즉시 14일치를 생성하여 유저에게 노출
      todoBatchService.generateInstancesRange(todo, todo.getStartDate(), todo.getStartDate().plusDays(14));
    }

    return TodoConverter.toCreateResponse(todo, null);
  }

  /**
   * [할 일 수정 및 변경사항 전파]
   * 반영 항목: 반복 규칙, 제목, 메모 등 수정된 모든 값
   */
  @Override
  @Transactional
  public TodoCreateResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
    Todo todo = todoRepository.findById(todoId)
        .orElseThrow(() -> new EntityNotFoundException("수정할 데이터를 찾을 수 없습니다."));

    User newAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId())
        .orElseThrow(() -> new EntityNotFoundException("지정된 담당자를 찾을 수 없습니다."))
        : todo.getDefaultAssignee();

    // 1. 마스터 수정
    todo.update(
        request.getTitle(),
        request.getMemo(),
        newAssignee,
        request.getIsRandom(),
        request.getRecurringType(),
        request.getRepeatDays(),
        request.getStartDate(),
        request.getEndDate()
    );

    // 2. 아직 완료되지 않은(PENDING) 미래 인스턴스들에게 변경사항 전파
    List<TodoInstance> futureInstances = todoInstanceRepository.findAllByTodoIdAndTargetDateAfter(todoId, LocalDate.now());
    futureInstances.forEach(instance -> instance.updateFromMaster(request.getTitle(), request.getMemo(), newAssignee));

    return TodoConverter.toCreateResponse(todo, null);
  }

  /**
   * [나의 할 일 전체 조회]
   */
  @Override
  public MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);

    Slice<TodoInstance> missedSlice = fetchMissedTodos(userId, today, missedCursor, pageRequest);
    Slice<TodoInstance> todaySlice = fetchTodayTodos(userId, today, todayCursor, pageRequest);
    Slice<TodoInstance> upcomingSlice = fetchUpcomingTodos(userId, today, upcomingCursor, pageRequest);

    return MyTodoListResponse.builder()
        .missedTodos(TodoConverter.toPagedResponse(missedSlice))
        .todayTodos(TodoConverter.toPagedResponse(todaySlice))
        .upcomingTodos(TodoConverter.toPagedResponse(upcomingSlice))
        .build();
  }

  /**
   * [나의 할 일 - 섹션별 개별 페이징 조회]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getMissedTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);
    Slice<TodoInstance> slice = fetchMissedTodos(userId, today, cursor, pageRequest);
    return TodoConverter.toPagedResponse(slice);
  }

  @Override
  public MyTodoListResponse.PagedTodoResponse getTodayTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);
    Slice<TodoInstance> slice = fetchTodayTodos(userId, today, cursor, pageRequest);
    return TodoConverter.toPagedResponse(slice);
  }

  @Override
  public MyTodoListResponse.PagedTodoResponse getUpcomingTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);
    Slice<TodoInstance> slice = fetchUpcomingTodos(userId, today, cursor, pageRequest);
    return TodoConverter.toPagedResponse(slice);
  }

  /**
   * [내 놓친 할 일 카운트 조회]
   */
  @Override
  public int getMissedTodoCount(Long userId) {
    return todoInstanceRepository.countMissedTodos(
        userId, LocalDate.now(), TodoStatus.PENDING);
  }

  /**
   * [우리 집 할 일 - 오늘 할 일 & 넛지 데이터]
   */
  @Override
  public RoomTodoResponse getRoomTodoList(Long userId) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    LocalDate today = LocalDate.now();

    // 1. 오늘 우리 집의 전체 인스턴스 조회
    List<TodoInstance> todayInstances = todoInstanceRepository.findRoomTodayTodos(roomId, today);

    // 2. 넛지 가이드에 필요한 개수 데이터 계산
    int totalCount = todayInstances.size();

    int completedCount = (int) todayInstances.stream()
        .filter(ti -> ti.getStatus() == TodoStatus.COMPLETED)
        .count();

    int myRemainingCount = (int) todayInstances.stream()
        .filter(ti -> ti.getActualAssignee().getId().equals(userId))
        .filter(ti -> ti.getStatus() == TodoStatus.PENDING)
        .count();

    // 3. TodoNudgeResponse 생성
    TodoNudgeResponse nudgeInfo = TodoNudgeResponse.builder()
        .totalRoomTodoCount(totalCount)
        .completedRoomTodoCount(completedCount)
        .myRemainingTodoCount(myRemainingCount)
        .build();

    // 4. 최종 RoomTodoResponse 반환
    return RoomTodoResponse.builder()
        .nudgeInfo(nudgeInfo)
        .todos(todayInstances.stream()
            .map(TodoConverter::toSummaryResponse)
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * [우리 집 할 일 - 지연된 할 일]
   * 리팩토링: 상태값이 DELAYED인 것을 찾는 게 아니라, 오늘 이전 날짜이면서 PENDING인 것을 조회
   */
  @Override
  public List<TodoSummaryResponse> getRoomDelayedTodo(Long userId) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    LocalDate today = LocalDate.now();
    return todoInstanceRepository.findRoomDelayedTodos(roomId, today, TodoStatus.PENDING)
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [우리 집 할 일 - 모든 할 일]
   */
  @Override
  public List<TodoSummaryResponse> getRoomAllTodo(Long userId) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    return todoInstanceRepository.findAllByRoomIdAndStatusNot(roomId, TodoStatus.COMPLETED)
        .stream()
        .sorted(Comparator.comparing(TodoInstance::getTargetDate)
            .thenComparing(TodoInstance::getCreatedAt))
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [특정 구성원의 할 일 조회]
   */
  @Override
  public List<TodoSummaryResponse> getMemberTodo(Long loginUserId, Long targetMemberId) {
    Long roomId = getActiveRoomMember(loginUserId).getRoomId();
    return todoInstanceRepository.findMemberTodos(roomId, targetMemberId)
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [완료된 할 일 모아보기]
   */
  @Override
  public Slice<TodoCompletedResponse> getCompletedTodos(Long userId, int page, int size) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    Slice<TodoInstance> completed = todoInstanceRepository.findCompletedTodos(
        roomId, TodoStatus.COMPLETED, PageRequest.of(page, size));
    return completed.map(TodoConverter::toCompletedDTO);
  }

  /**
   * [할 일 완료 처리 - S3 연동]
   */
  @Override
  @Transactional
  public void completeTodo(Long userId, Long instanceId, MultipartFile image) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new EntityNotFoundException("해당 할 일 일정을 찾을 수 없습니다."));

    if (instance.getActualAssignee().getId().longValue() != userId.longValue()) {
      throw new AccessDeniedException("본인에게 할당된 할 일만 완료할 수 있습니다.");
    }

    String newImageUrl = instance.getImageUrl();

    if (image != null && !image.isEmpty()) {
      try {
        if (instance.getImageUrl() != null) {
          s3Uploader.deleteFile(instance.getImageUrl());
        }
        newImageUrl = s3Uploader.upload(image, "todo");
      } catch (IOException e) {
        throw new RuntimeException("이미지 업로드 중 서버 오류가 발생했습니다.");
      }
    }

    instance.complete(newImageUrl);
  }

  /**
   * [할 일 인증샷 삭제]
   */
  @Override
  @Transactional
  public void deleteTodoImage(Long userId, Long instanceId) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new EntityNotFoundException("해당 할 일 일정을 찾을 수 없습니다."));

    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new AccessDeniedException("본인의 인증샷만 삭제할 수 있습니다.");
    }

    if (instance.getImageUrl() != null) {
      s3Uploader.deleteFile(instance.getImageUrl());
    }

    instance.removeImage();
  }

  // --- 헬퍼 메서드 ---

  private TodoInstance saveInstance(Todo todo, Room room, User assignee, LocalDate date) {
    TodoInstance instance = TodoInstance.builder()
        .todo(todo)
        .room(room)
        .actualAssignee(assignee)
        .title(todo.getTitle())
        .memo(todo.getMemo())
        .targetDate(date)
        .status(TodoStatus.PENDING)
        .build();
    return todoInstanceRepository.save(instance);
  }

  private Slice<TodoInstance> fetchMissedTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    LocalDate cursorDate;
    Long cursorId;

    if (cursor == null || cursor.isBlank()) {
      cursorDate = LocalDate.of(9999, 12, 31);
      cursorId = Long.MAX_VALUE;
    } else {
      String[] parts = cursor.split("_");
      cursorDate = LocalDate.parse(parts[0]);
      cursorId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findMissedTodosWithCursor(userId, today, cursorDate, cursorId, pageable);
  }

  private Slice<TodoInstance> fetchTodayTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    Long cursorId = 0L;
    if (cursor != null && !cursor.isBlank()) {
      cursorId = Long.parseLong(cursor);
    }
    return todoInstanceRepository.findTodayTodosWithCursor(userId, today, cursorId, pageable);
  }

  private Slice<TodoInstance> fetchUpcomingTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    LocalDate cursorDate = today.plusDays(1);
    Long cursorId;

    if (cursor == null || cursor.isBlank()) {
      cursorDate = today;
      cursorId = 0L;
    } else {
      String[] parts = cursor.split("_");
      cursorDate = LocalDate.parse(parts[0]);
      cursorId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findUpcomingTodosWithCursor(userId, today, cursorDate, cursorId, pageable);
  }

  private RoomMember getActiveRoomMember(Long userId) {
    return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private Room findRoomById(Long roomId) {
    return roomRepository.findByIdAndDeletedAtIsNull(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }
}