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
import com.example.dzipsa.domain.todo.dto.response.TodoDetailResponse;
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
import com.example.dzipsa.global.exception.domain.TodoErrorCode;
import com.example.dzipsa.global.util.S3Uploader;
import java.io.IOException;
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

    // 1. 기본 설정 및 Todo 저장 (기존 코드 동일)
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
   * 오늘 이전 날짜이면서 PENDING인 것을 조회
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

  /**
   * 할 일 상세 조회
   * @param userId 조회 요청자 ID (본인 여부 확인용)
   * @param instanceId 조회할 특정 일자 할 일(Instance)의 ID
   * @return 할 일 상세 정보 (상태, 반복정보, 담당자 포함)
   */
  @Override
  @Transactional(readOnly = true)
  public TodoDetailResponse getTodoDetail(Long userId, Long instanceId) {
    // 1. DB에서 해당 Instance 존재 여부 확인
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    // 2. Converter를 통해 엔티티를 응답 DTO로 변환
    // (상태값 계산 및 반복 주기 한글화 로직은 Converter 내부에서 수행)
    return TodoConverter.toDetailResponse(instance, userId);
  }

  /**
   * 할 일 상태 초기화 (완료 -> 진행 중으로 변경)
   * * @param userId 요청자 ID (권한 검증용)
   * @param instanceId 상태를 변경할 할 일 ID
   */
  @Override
  @Transactional
  public void resetTodoStatus(Long userId, Long instanceId) {
    // 1. 해당 Instance 데이터 조회
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new BusinessException(TodoErrorCode.TODO_INSTANCE_NOT_FOUND));

    // 2. 권한 검증: 현재 할 일의 실제 담당자(Assignee)와 요청한 사용자가 일치하는지 확인
    // (본인의 할 일만 상태를 '진행 중'으로 되돌릴 수 있음)
    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new BusinessException(TodoErrorCode.FORBIDDEN_UPDATE_LIMIT);
    }

    // 3. 엔티티 상태 변경
    // TodoStatus를 PENDING(진행 중)으로 변경하고, 완료 일시(completedAt)를 초기화함
    instance.resetToPending();
  }

  // --- 헬퍼 메서드 ---

  /**
   * [할 일 인스턴스 생성 및 저장]
   * 마스터(Todo) 설정에 따라 실제 수행할 날짜별 인스턴스를 생성함
   */
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

  /**
   * [놓친 할 일 목록 페이징 조회]
   * 오늘 이전 날짜 중 완료되지 않은 항목을 커서 기반으로 조회
   */
  private Slice<TodoInstance> fetchMissedTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    LocalDate cursorDate;
    Long cursorId;

    // 첫 페이지 호출 시 (커서 없음) 가장 먼 미래 날짜와 최대 ID로 초기값 설정
    if (cursor == null || cursor.isBlank()) {
      cursorDate = LocalDate.of(9999, 12, 31);
      cursorId = Long.MAX_VALUE;
    } else {
      // 커서 파싱 (날짜_ID 형태)
      String[] parts = cursor.split("_");
      cursorDate = LocalDate.parse(parts[0]);
      cursorId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findMissedTodosWithCursor(userId, today, cursorDate, cursorId, pageable);
  }

  /**
   * [오늘의 할 일 목록 페이징 조회]
   * 오늘 날짜에 배정된 할 일을 ID 기반 커서로 조회
   */
  private Slice<TodoInstance> fetchTodayTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    Long cursorId = 0L;
    if (cursor != null && !cursor.isBlank()) {
      cursorId = Long.parseLong(cursor);
    }
    return todoInstanceRepository.findTodayTodosWithCursor(userId, today, cursorId, pageable);
  }

  /**
   * [예정된 할 일 목록 페이징 조회]
   * 내일부터 발생할 할 일들을 날짜 및 ID 커서 기반으로 조회
   */
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

  /**
   * [활성 룸 멤버 조회]
   * 사용자가 현재 소속되어 있는 집(Room)의 멤버 정보를 가져옴 (탈퇴 제외)
   */
  private RoomMember getActiveRoomMember(Long userId) {
    return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  /**
   * [유효한 룸 조회]
   * 삭제되지 않은 방(Room) 정보를 ID로 조회
   */
  private Room findRoomById(Long roomId) {
    return roomRepository.findByIdAndDeletedAtIsNull(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  /**
   * 시작일과 종료일의 논리적 타당성 검증
   */
  private void validateTodoDates(LocalDate start, LocalDate end) {
    if (end != null && start.isAfter(end)) {
      throw new BusinessException(TodoErrorCode.INVALID_DATE_RANGE);
    }
  }

  /**
   * 반복 규칙을 분석하여 가장 가까운 미래의 마감일을 계산
   */
  private LocalDate calculateFirstRecurringDate(Todo todo) {
    LocalDate start = todo.getStartDate();
    RecurringType type = todo.getRecurringType();
    String days = todo.getRepeatDays();

    if (type == RecurringType.NONE || days == null || days.isBlank()) {
      return start;
    }

    if (type == RecurringType.MONTHLY) {
      try {
        int dayOfMonth = Integer.parseInt(days.trim());
        // 시작일의 날짜를 설정된 날짜로 맞춤
        LocalDate firstDate = start.withDayOfMonth(Math.min(dayOfMonth, start.lengthOfMonth()));
        // 만약 맞춘 날짜가 시작일보다 전이면 다음 달로 넘김
        if (firstDate.isBefore(start)) {
          firstDate = firstDate.plusMonths(1);
          // 다음 달에도 해당 날짜가 존재하는지 확인 (예: 31일 설정 시 2월 처리)
          firstDate = firstDate.withDayOfMonth(Math.min(dayOfMonth, firstDate.lengthOfMonth()));
        }
        return firstDate;
      } catch (NumberFormatException e) {
        return start;
      }
    }

    // 주간 반복 등 다른 케이스는 일단 시작일 반환
    return start;
  }
}