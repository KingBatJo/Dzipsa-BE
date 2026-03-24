package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.todo.converter.TodoConverter;
import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoDeleteRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.*;
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
import com.example.dzipsa.global.exception.domain.TodoErrorCode;
import com.example.dzipsa.global.util.S3Uploader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
  private final TodoBatchService todoBatchService;

  /**
   * [할 일 신규 등록]
   * 1. 할 일의 메인 설정(마스터)을 저장
   * 2. 반복 설정이 있는 경우 즉시 14일치 인스턴스를 생성
   */
  @Override
  @Transactional
  public TodoCreateResponse createTodo(User user, TodoCreateRequest request) {

    // 날짜 유효성 검증 (시작일이 종료일보다 늦으면 예외 발생)
    validateTodoDates(request.getStartDate(), request.getEndDate());

    // 1. 기본 설정 및 Todo 저장
    RoomMember myMember = getActiveRoomMember(user.getId());
    Room myRoom = findRoomById(myMember.getRoomId());

    User targetAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId())
        .orElseThrow(() -> new BusinessException(TodoErrorCode.ASSIGNEE_NOT_FOUND))
        : user;

    // 반복 여부에 따른 시작 날짜 결정 (NONE이면 targetDate가 곧 시작일)
    LocalDate finalStartDate = (request.getRecurringType() == RecurringType.NONE)
        ? request.getTargetDate()
        : request.getStartDate();

    Todo todo = Todo.builder()
        .room(myRoom)
        .writer(user)
        .defaultAssignee(targetAssignee)
        .title(request.getTitle())
        .memo(request.getMemo())
        .isRandom(request.getIsRandom())
        .startDate(finalStartDate)
        .endDate(request.getEndDate())
        .recurringType(request.getRecurringType())
        .repeatDays(request.getRepeatDays())
        .isActive(true)
        .build();
    todoRepository.save(todo);

    // 2. 인스턴스 생성 및 응답에 보낼 인스턴스 추출
    TodoInstance representInstance = null;

    if (todo.getRecurringType() == RecurringType.NONE) {
      // 반복 없음: 생성된 단일 인스턴스를 바로 할당 (사용자가 입력한 targetDate 기준)
      representInstance = saveInstance(todo, myRoom, targetAssignee, request.getTargetDate());
    } else {
      // 반복 설정: 14일치 생성 후 그중 오늘 날짜인 것이 있다면 응답에 포함
      todoBatchService.generateInstancesRange(todo, todo.getStartDate(), todo.getStartDate().plusDays(14));

      // 생성된 여러 인스턴스 중 날짜가 가장 빠른(가까운) 것을 하나 가져오기
      representInstance = todoInstanceRepository.findAllByTodoIdAndTargetDateAfter(todo.getId(), LocalDate.now().minusDays(1))
          .stream()
          .min(Comparator.comparing(TodoInstance::getTargetDate))
          .orElse(null);
    }

    // 응답용 targetDate 직접 계산 (로직 분리)
    LocalDate responseTargetDate = (representInstance != null)
        ? representInstance.getTargetDate()
        : calculateFirstRecurringDate(todo);

    // 3. 추출한 인스턴스를 컨버터에 전달
    return TodoConverter.toCreateResponse(todo, representInstance, responseTargetDate);
  }

  /**
   * [할 일 수정 및 변경사항 전파]
   * 반영 항목: 반복 규칙, 제목, 메모 등 수정된 모든 값
   */
  @Override
  @Transactional
  public TodoCreateResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {

    // 날짜 유효성 검증 (시작일이 종료일보다 늦으면 예외 발생)
    validateTodoDates(request.getStartDate(), request.getEndDate());

    // 1. 마스터 데이터 조회
    Todo todo = todoRepository.findById(todoId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_NOT_FOUND));

    // 2. 담당자 확인
    User newAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId())
        .orElseThrow(() -> new BusinessException(TodoErrorCode.ASSIGNEE_NOT_FOUND))
        : todo.getDefaultAssignee();

    // 수정 시 반복 여부에 따른 시작 날짜 재결정
    LocalDate finalStartDate = (request.getRecurringType() == RecurringType.NONE)
        ? request.getTargetDate()
        : request.getStartDate();

    // 3. Todo 마스터 정보 업데이트
    todo.update(
        request.getTitle(),
        request.getMemo(),
        newAssignee,
        request.getIsRandom(),
        request.getRecurringType(),
        request.getRepeatDays(),
        finalStartDate,
        request.getEndDate()
    );

    // 기존 미래 인스턴스(오늘 포함) 정리 후 재생성
    todoInstanceRepository.deleteFutureInstances(todo.getId(), LocalDate.now());

    // 4. 요일이나 규칙이 바뀌었을 수 있으므로 배치 서비스를 호출해 인스턴스들을 재정비
    if (todo.getRecurringType() == RecurringType.NONE) {
      // 반복 없음: 수정된 targetDate에 단일 생성
      saveInstance(todo, todo.getRoom(), newAssignee, request.getTargetDate());
    } else {
      // 오늘부터 향후 14일간의 데이터를 수정된 규칙에 맞게 생성/갱신
      todoBatchService.generateInstancesRange(todo, LocalDate.now(), LocalDate.now().plusDays(14));
    }

    // 5. 수정한 직후 오늘 수행해야 할 인스턴스가 있는지 다시 조회
    TodoInstance representInstance = todoInstanceRepository.findByTodoIdAndTargetDate(todo.getId(), LocalDate.now())
        .orElse(null);

    // 응답용 targetDate 직접 계산 (로직 분리)
    LocalDate responseTargetDate = (representInstance != null)
        ? representInstance.getTargetDate()
        : calculateFirstRecurringDate(todo);

    return TodoConverter.toCreateResponse(todo, representInstance, responseTargetDate);
  }

  /**
   * [나의 할 일 전체 조회]
   */
  @Override
  public MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor) {
    LocalDate today = LocalDate.now();

    log.info("=== [내 할 일 조회 시작] ===");
    log.info("로그인 유저 ID: {}, 오늘 날짜: {}", userId, today);

    PageRequest pageRequest = PageRequest.of(0, 5);

    Slice<TodoInstance> missedSlice = fetchMissedTodos(userId, today, missedCursor, pageRequest);
    Slice<TodoInstance> todaySlice = fetchTodayTodos(userId, today, todayCursor, pageRequest);
    Slice<TodoInstance> upcomingSlice = fetchUpcomingTodos(userId, today, upcomingCursor, pageRequest);

    return MyTodoListResponse.builder()
        .missedTodos(toPagedResponseWithDate(missedSlice))
        .todayTodos(toPagedResponseWithId(todaySlice))
        .upcomingTodos(toPagedResponseWithDate(upcomingSlice))
        .build();
  }

  /**
   * [나의 할 일 - 섹션별 개별 페이징 조회]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getMissedTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 5);
    Slice<TodoInstance> slice = fetchMissedTodos(userId, today, cursor, pageRequest);
    return toPagedResponseWithDate(slice);
  }

  @Override
  public MyTodoListResponse.PagedTodoResponse getTodayTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 5);
    Slice<TodoInstance> slice = fetchTodayTodos(userId, today, cursor, pageRequest);
    return toPagedResponseWithId(slice);
  }

  @Override
  public MyTodoListResponse.PagedTodoResponse getUpcomingTodos(Long userId, String cursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 5);
    Slice<TodoInstance> slice = fetchUpcomingTodos(userId, today, cursor, pageRequest);
    return toPagedResponseWithDate(slice);
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
   * [우리 집 할 일 - 상단 넛지 및 섹션별 숫자 통계]
   */
  @Override
  public TodoNudgeResponse getRoomTodoStats(Long userId) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    LocalDate today = LocalDate.now();

    // 1. 넛지 가이드 및 섹션별 전체 카운트 계산
    List<TodoInstance> totalTodayInstances = todoInstanceRepository.findRoomTodayTodos(roomId, today);

    int totalTodayCount = totalTodayInstances.size();
    int completedTodayCount = (int) totalTodayInstances.stream()
        .filter(ti -> ti.getStatus() == TodoStatus.COMPLETED).count();
    int myRemainingCount = (int) totalTodayInstances.stream()
        .filter(ti -> ti.getActualAssignee().getId().equals(userId))
        .filter(ti -> ti.getStatus() == TodoStatus.PENDING).count();
    int todayPendingCount = (int) totalTodayInstances.stream()
        .filter(ti -> ti.getStatus() == TodoStatus.PENDING).count();

    int delayedCount = (int) todoInstanceRepository.countRoomDelayedTodos(roomId, today, TodoStatus.PENDING);
    int allPendingCount = (int) todoInstanceRepository.countRoomAllTodos(roomId, TodoStatus.COMPLETED);

    // 2. 구성원별 오늘 남은 할 일 통계 추출
    List<RoomMember> members = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(roomId);
    List<RoomTodoResponse.MemberTodoStatsResponse> memberStats = members.stream()
        .filter(member -> !member.getUserId().equals(userId))
        .map(member -> {
          User u = userRepository.findById(member.getUserId())
              .orElseThrow(() -> new BusinessException(TodoErrorCode.ASSIGNEE_NOT_FOUND));

          int remaining = todoInstanceRepository.countTotalPendingByMember(u.getId(), TodoStatus.PENDING);

          return RoomTodoResponse.MemberTodoStatsResponse.builder()
              .userId(u.getId())
              .nickname(u.getNickname())
              .profileImageUrl(u.getProfileImageUrl())
              .remainingCount(remaining)
              .build();
        }).collect(Collectors.toList());

    return TodoNudgeResponse.builder()
        .totalRoomTodoCount(totalTodayCount)
        .completedRoomTodoCount(completedTodayCount)
        .myRemainingTodoCount(myRemainingCount)
        .todayTotalCount(todayPendingCount)
        .delayedTotalCount(delayedCount)
        .allTotalCount(allPendingCount)
        .memberStats(memberStats)
        .build();
  }

  /**
   * [우리 집 할 일 - 오늘 할 일 리스트만 조회]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getRoomTodayTodoList(Long userId, String cursor) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    LocalDate today = LocalDate.now();

    PageRequest pageRequest = PageRequest.of(0, 10);
    Long cursorId = (cursor == null || cursor.isBlank()) ? 0L : Long.parseLong(cursor);

    Slice<TodoInstance> todaySlice = todoInstanceRepository.findRoomTodayTodosWithCursor(
        roomId, today, TodoStatus.PENDING, cursorId, pageRequest);

    return toPagedResponseWithId(todaySlice);
  }

  /**
   * [우리 집 할 일 - 지연된 할 일]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getRoomDelayedTodo(Long userId, String cursor) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);

    LocalDate cursorDate;
    Long cursorId;

    if (cursor == null || cursor.isBlank()) {
      cursorDate = LocalDate.of(1900, 1, 1);
      cursorId = 0L;
    } else {
      try {
        String[] parts = cursor.split("_");
        cursorDate = LocalDate.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        cursorDate = LocalDate.of(1900, 1, 1);
        cursorId = 0L;
      }
    }

    Slice<TodoInstance> slice = todoInstanceRepository.findRoomDelayedTodosWithCursor(
        roomId, today, TodoStatus.PENDING, cursorDate, cursorId, pageRequest);

    return toPagedResponseWithDate(slice);
  }

  /**
   * [우리 집 할 일 - 모든 할 일]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getRoomAllTodo(Long userId, String cursor) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    PageRequest pageRequest = PageRequest.of(0, 10);

    LocalDate cursorDate;
    Long cursorId;

    // 모든 할 일은 날짜가 섞이므로 날짜_ID 기반 커서 파싱
    if (cursor == null || cursor.isBlank()) {
      cursorDate = LocalDate.of(1900, 1, 1);
      cursorId = 0L;
    } else {
      try {
        String[] parts = cursor.split("_");
        cursorDate = LocalDate.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        cursorDate = LocalDate.of(1900, 1, 1);
        cursorId = 0L;
      }
    }

    Slice<TodoInstance> slice = todoInstanceRepository.findRoomAllTodosWithCursor(
        roomId, TodoStatus.COMPLETED, cursorDate, cursorId, pageRequest);

    // [수정] WithDate를 호출하여 응답 커서가 "날짜_ID" 형식이 되도록 강제
    return toPagedResponseWithDate(slice);
  }

  /**
   * [특정 구성원의 할 일 조회]
   */
  @Override
  public MyTodoListResponse.PagedTodoResponse getMemberTodo(Long loginUserId, Long targetMemberId, String cursor) {
    Long roomId = getActiveRoomMember(loginUserId).getRoomId();
    PageRequest pageRequest = PageRequest.of(0, 10);

    LocalDate cursorDate;
    Long cursorId;

    // 구성원 할 일도 여러 날짜가 포함되므로 날짜_ID 기반 커서 파싱
    if (cursor == null || cursor.isBlank()) {
      cursorDate = LocalDate.of(1900, 1, 1);
      cursorId = 0L;
    } else {
      try {
        String[] parts = cursor.split("_");
        cursorDate = LocalDate.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        cursorDate = LocalDate.of(1900, 1, 1);
        cursorId = 0L;
      }
    }

    Slice<TodoInstance> slice = todoInstanceRepository.findMemberTodosWithCursor(
        roomId, targetMemberId, cursorDate, cursorId, pageRequest);

    // WithDate를 호출하여 응답 커서가 "날짜_ID" 형식이 되도록 강제
    return toPagedResponseWithDate(slice);
  }

  /**
   * [완료된 할 일 목록 조회]
   */
  @Override
  @Transactional(readOnly = true)
  public MyTodoListResponse.PagedTodoResponse getCompletedTodos(Long userId, String cursor) {
    Long roomId = getActiveRoomMember(userId).getRoomId();
    PageRequest pageRequest = PageRequest.of(0, 10);

    LocalDateTime cursorDateTime = null;
    Long cursorId = null;

    if (cursor != null && !cursor.isBlank()) {
      try {
        String[] parts = cursor.split("_");
        cursorDateTime = LocalDateTime.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        log.error("잘못된 커서 형식: {}", cursor);
      }
    }

    Slice<TodoInstance> slice = todoInstanceRepository.findCompletedTodosWithCursor(
        roomId, TodoStatus.COMPLETED, cursorDateTime, cursorId, pageRequest);

    List<TodoSummaryResponse> content = slice.getContent().stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());

    String nextCursor = null;
    if (slice.hasNext() && !content.isEmpty()) {
      TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);
      nextCursor = String.format("%s_%d", lastItem.getCompletedAt().toString(), lastItem.getId());
    }

    return MyTodoListResponse.PagedTodoResponse.builder()
        .content(content)
        .hasNext(slice.hasNext())
        .nextCursor(nextCursor)
        .build();
  }

  /**
   * [할 일 완료 처리 - S3 연동]
   */
  @Override
  @Transactional
  public void completeTodo(Long userId, Long instanceId, MultipartFile image) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    if (instance.getActualAssignee().getId().longValue() != userId.longValue()) {
      throw new BusinessException(TodoErrorCode.FORBIDDEN_UPDATE_LIMIT);
    }

    String newImageUrl = instance.getImageUrl();

    if (image != null && !image.isEmpty()) {
      try {
        if (instance.getImageUrl() != null) {
          s3Uploader.deleteFile(instance.getImageUrl());
        }
        newImageUrl = s3Uploader.upload(image, "todo");
      } catch (IOException e) {
        throw new BusinessException(TodoErrorCode.IMAGE_UPLOAD_FAILED);
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
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new BusinessException(TodoErrorCode.FORBIDDEN_IMAGE_DELETE);
    }

    if (instance.getImageUrl() != null) {
      s3Uploader.deleteFile(instance.getImageUrl());
    }

    instance.removeImage();
  }

  @Override
  @Transactional(readOnly = true)
  public TodoDetailResponse getTodoDetail(Long userId, Long instanceId) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    if (!instance.getTodo().getIsActive()) {
      throw new BusinessException(TodoErrorCode.TODO_ALREADY_DELETED);
    }

    return TodoConverter.toDetailResponse(instance, userId);
  }

  @Override
  @Transactional
  public void resetTodoStatus(Long userId, Long instanceId) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new BusinessException(TodoErrorCode.FORBIDDEN_UPDATE_LIMIT);
    }

    instance.resetToPending();
  }

  /**
   * [할 일 삭제]
   */
  @Override
  @Transactional
  public void deleteTodo(Long userId, Long instanceId, TodoDeleteRequest request) {
    TodoInstance targetInstance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    Todo todo = targetInstance.getTodo();
    LocalDate targetDate = targetInstance.getTargetDate();

    validateDeletePermission(userId, targetInstance);

    switch (request.getScope()) {
      case ONLY_THIS:
        todoInstanceRepository.delete(targetInstance);
        break;

      case SINCE_THIS:
        todoInstanceRepository.deleteAllByTodoIdAndTargetDateGreaterThanEqualAndStatus(
            todo.getId(), targetDate, TodoStatus.PENDING);
        todo.updateEndDate(targetDate.minusDays(1));
        break;

      case ALL_RECURRING:
        todoInstanceRepository.deleteAllByTodoIdAndStatus(todo.getId(), TodoStatus.PENDING);
        todo.updateIsActive(false);
        break;
    }
  }

  // --- 헬퍼 메서드 ---

  private MyTodoListResponse.PagedTodoResponse toPagedResponseWithId(Slice<TodoInstance> slice) {
    MyTodoListResponse.PagedTodoResponse response = TodoConverter.toPagedResponse(slice);
    if (slice.hasNext() && !slice.getContent().isEmpty()) {
      TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);
      response.setNextCursor(String.valueOf(lastItem.getId()));
    }
    return response;
  }

  private MyTodoListResponse.PagedTodoResponse toPagedResponseWithDate(Slice<TodoInstance> slice) {
    MyTodoListResponse.PagedTodoResponse response = TodoConverter.toPagedResponse(slice);
    if (slice.hasNext() && !slice.getContent().isEmpty()) {
      TodoInstance lastItem = slice.getContent().get(slice.getContent().size() - 1);
      response.setNextCursor(lastItem.getTargetDate().toString() + "_" + lastItem.getId());
    }
    return response;
  }

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
      cursorDate = LocalDate.of(1900, 1, 1);
      cursorId = 0L;
    } else {
      try {
        String[] parts = cursor.split("_");
        cursorDate = LocalDate.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        cursorDate = LocalDate.of(1900, 1, 1);
        cursorId = 0L;
      }
    }
    return todoInstanceRepository.findMissedTodosWithCursor(userId, today, cursorDate, cursorId, pageable);
  }

  private Slice<TodoInstance> fetchTodayTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    Long cursorId = 0L;
    if (cursor != null && !cursor.isBlank()) {
      try {
        cursorId = Long.parseLong(cursor);
      } catch (NumberFormatException e) {
        cursorId = 0L;
      }
    }
    return todoInstanceRepository.findTodayTodosWithCursor(userId, today, TodoStatus.PENDING, cursorId, pageable);
  }

  private Slice<TodoInstance> fetchUpcomingTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    LocalDate cursorDate;
    Long cursorId;

    if (cursor == null || cursor.isBlank()) {
      cursorDate = today;
      cursorId = 0L;
    } else {
      try {
        String[] parts = cursor.split("_");
        cursorDate = LocalDate.parse(parts[0]);
        cursorId = Long.parseLong(parts[1]);
      } catch (Exception e) {
        cursorDate = today;
        cursorId = 0L;
      }
    }
    return todoInstanceRepository.findUpcomingTodosWithCursor(userId, today, TodoStatus.PENDING, cursorDate, cursorId, pageable);
  }

  private RoomMember getActiveRoomMember(Long userId) {
    return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private Room findRoomById(Long roomId) {
    return roomRepository.findByIdAndDeletedAtIsNull(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private void validateTodoDates(LocalDate start, LocalDate end) {
    if (end != null && start.isAfter(end)) {
      throw new BusinessException(TodoErrorCode.INVALID_DATE_RANGE);
    }
  }

  private LocalDate calculateFirstRecurringDate(Todo todo) {
    LocalDate start = todo.getStartDate();
    RecurringType type = todo.getRecurringType();
    String days = todo.getRepeatDays();

    if (type == RecurringType.NONE || days == null || days.isBlank()) {
      return start;
    }

    // 매월 반복일 경우: 29~31일 선택 시 말일 처리 로직
    if (type == RecurringType.MONTHLY) {
      try {
        int dayOfMonth = Integer.parseInt(days.trim());

        // 1. 시작 날짜가 속한 달의 길이를 확인
        int lastDayOfStartMonth = start.lengthOfMonth();

        // 2. 선택한 날짜가 해당 월의 마지막 날보다 크면 말일로 조정 (예: 2월 31일 선택 시 28일/29일)
        LocalDate firstDate = start.withDayOfMonth(Math.min(dayOfMonth, lastDayOfStartMonth));

        // 3. 만약 계산된 날짜가 시작일보다 전이라면 다음 달로 넘겨서 다시 계산
        if (firstDate.isBefore(start)) {
          firstDate = start.plusMonths(1);
          int lastDayOfNextMonth = firstDate.lengthOfMonth();
          firstDate = firstDate.withDayOfMonth(Math.min(dayOfMonth, lastDayOfNextMonth));
        }
        return firstDate;
      } catch (NumberFormatException e) {
        return start;
      }
    }
    return start;
  }

  private void validateDeletePermission(Long userId, TodoInstance instance) {
    boolean isAssignee = instance.getActualAssignee().getId().equals(userId);
    if (!isAssignee) {
      throw new BusinessException(TodoErrorCode.FORBIDDEN_DELETE);
    }
  }
}