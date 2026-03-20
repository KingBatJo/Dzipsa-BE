package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.todo.converter.TodoConverter;
import com.example.dzipsa.domain.todo.dto.request.TodoCreateRequest;
import com.example.dzipsa.domain.todo.dto.request.TodoUpdateRequest;
import com.example.dzipsa.domain.todo.dto.response.MyTodoListResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoCompletedResponse;
import com.example.dzipsa.domain.todo.dto.response.TodoSummaryResponse;
import com.example.dzipsa.domain.todo.entity.Todo;
import com.example.dzipsa.domain.todo.entity.TodoInstance;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.todo.repository.TodoInstanceRepository;
import com.example.dzipsa.domain.todo.repository.TodoRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.util.S3Uploader;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
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

  /**
   * [할 일 신규 등록]
   * 1. 할 일의 메인 설정(마스터)을 저장
   * 2. 사용자 리스트에 즉시 노출되도록 시작일 기준의 첫 번째 실행 인스턴스를 생성
   */
  @Override
  @Transactional
  public void createTodo(Long userId, Long roomId, TodoCreateRequest request) {
    User writer = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("작성자(ID: " + userId + ")를 찾을 수 없습니다."));
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new RuntimeException("해당 방(ID: " + roomId + ")이 존재하지 않습니다."));

    // 담당자가 미지정된 경우 작성자를 기본 담당자로 설정
    User defaultAssignee = (request.getDefaultAssigneeId() != null)
        ? userRepository.findById(request.getDefaultAssigneeId()).orElse(writer)
        : writer;

    // 1. Todo 마스터 생성
    Todo todo = Todo.builder()
        .room(room).writer(writer).defaultAssignee(defaultAssignee)
        .title(request.getTitle()).memo(request.getMemo())
        .isRandom(request.getIsRandom()).startDate(request.getStartDate())
        .endDate(request.getEndDate()).recurringType(request.getRecurringType())
        .repeatDays(request.getRepeatDays()).isActive(true)
        .build();
    todoRepository.save(todo);

    // 2. 첫 실행 인스턴스 즉시 생성
    saveInstance(todo, room, defaultAssignee, request.getStartDate());
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
   * [우리 집 오늘 할 일 전체 조회]
   */
  @Override
  public List<TodoSummaryResponse> getRoomTodoList(Long roomId) {
    // 오늘 날짜이면서 미완료(PENDING) 우선, 그 다음 생성순 정렬
    return todoInstanceRepository.findRoomTodayTodos(roomId, LocalDate.now())
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [우리 집 지연된 할 일 전체 조회]
   */
  @Override
  public List<TodoSummaryResponse> getRoomDelayedTodo(Long roomId) {
    LocalDate today = LocalDate.now();
    // 오늘 이전 날짜이면서 미완료인 것들을 오래된 날짜순(Asc)으로 정렬 (D+5, D+3...)
    return todoInstanceRepository.findRoomDelayedTodos(
            roomId, today, TodoStatus.PENDING)
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [우리 집 모든 할 일 전체 조회]
   */
  @Override
  public List<TodoSummaryResponse> getRoomAllTodo(Long roomId) {
    // 오늘을 포함하여 모든 미완료 및 오늘 완료된 건을 최신순으로 조회
    return todoInstanceRepository.findRoomAllTodos(
            roomId, LocalDate.now())
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [특정 구성원의 할 일 조회]
   */
  @Override
  public List<TodoSummaryResponse> getMemberTodo(Long roomId, Long userId) {
    // 특정 유저에게 할당된 오늘까지의 할 일을 마감일순으로 조회
    return todoInstanceRepository.findMemberTodos(
            roomId, userId, LocalDate.now())
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [완료된 할 일 모아보기]
   */
  @Override
  public Slice<TodoCompletedResponse> getCompletedTodos(Long roomId, int page, int size) {
    Slice<TodoInstance> completed = todoInstanceRepository.findCompletedTodos(
        roomId, TodoStatus.COMPLETED, PageRequest.of(page, size));
    return completed.map(TodoConverter::toCompletedDTO);
  }

  /**
   * [할 일 완료 처리 - S3 연동]
   * 사진 업로드 시 확장자 체크(S3Uploader) 및 예외 처리가 포함됨
   * 새로운 사진 업로드 시 기존 사진이 있다면 S3에서 자동 삭제함
   */
  @Override
  @Transactional
  public void completeTodo(Long userId, Long instanceId, MultipartFile image) {
    // 1. 해당 할 일 실행 단위(Instance) 조회
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new EntityNotFoundException("해당 할 일 일정을 찾을 수 없습니다."));

    // 2. 권한 확인 (본인 담당인지 확인)
    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new AccessDeniedException("본인에게 할당된 할 일만 완료할 수 있습니다.");
    }

    // 3. 사진 처리 로직
    String newImageUrl = instance.getImageUrl(); // 기본값은 기존 URL 유지

    if (image != null && !image.isEmpty()) {
      try {
        // 기존 이미지가 이미 등록되어 있다면 S3에서 먼저 삭제 (비용 절감)
        if (instance.getImageUrl() != null) {
          s3Uploader.deleteFile(instance.getImageUrl());
        }

        // S3Uploader 내부에서 확장자 체크를 수행함
        newImageUrl = s3Uploader.upload(image, "todo");
      } catch (IllegalArgumentException e) {
        log.warn("이미지 확장자 불일치: {}", e.getMessage());
        throw new IllegalArgumentException(e.getMessage());
      } catch (IOException e) {
        log.error("S3 이미지 업로드 중 IO 오류 발생: {}", e.getMessage());
        throw new RuntimeException("이미지 업로드 중 서버 오류가 발생했습니다.");
      }
    }

    // 4. 엔티티 상태 변경
    instance.complete(newImageUrl);
  }

  /**
   * [할 일 인증샷 삭제]
   * '-' 버튼 클릭 시 호출: S3 파일 삭제 및 DB URL을 null로 초기화
   */
  @Override
  @Transactional
  public void deleteTodoImage(Long userId, Long instanceId) {
    TodoInstance instance = todoInstanceRepository.findById(instanceId)
        .orElseThrow(() -> new EntityNotFoundException("해당 할 일 일정을 찾을 수 없습니다."));

    // 본인 확인
    if (!instance.getActualAssignee().getId().equals(userId)) {
      throw new AccessDeniedException("본인의 인증샷만 삭제할 수 있습니다.");
    }

    // 1. S3에서 실제 파일 삭제
    if (instance.getImageUrl() != null) {
      s3Uploader.deleteFile(instance.getImageUrl());
    }

    // 2. DB URL 삭제 (TodoInstance 엔티티에 구현된 removeImage 호출)
    instance.removeImage();
  }

  /**
   * [할 일 수정 및 변경사항 전파]
   * 반영 항목: 반복 규칙, 제목, 메모 등 수정된 모든 값
   */
  @Override
  @Transactional
  public void updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
    Todo todo = todoRepository.findById(todoId)
        .orElseThrow(() -> new EntityNotFoundException("수정할 원본 데이터가 없습니다.")); // 에러코드 적용 대상

    User newAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId()).orElse(todo.getDefaultAssignee())
        : todo.getDefaultAssignee();

    // 1. 마스터 정보 수정 (파라미터 추가)
    todo.update(request.getTitle(), request.getMemo(), newAssignee, request.getIsRandom(),
        request.getRecurringType(), request.getRepeatDays(), request.getStartDate(), request.getEndDate());

    // 2. 미래 인스턴스 업데이트
    List<TodoInstance> futureInstances = todoInstanceRepository.findAllByTodoIdAndTargetDateAfter(todoId, LocalDate.now());
    futureInstances.forEach(instance -> {
      instance.updateFromMaster(request.getTitle(), request.getMemo(), newAssignee);
    });
  }

  /**
   * [반복 일정 자동 생성 배치]
   * 생성 시점에 2주치(14일분) 일정을 미리 생성
   */
  @Override
  @Transactional
  public void generateRecurringTodos() {
    LocalDate today = LocalDate.now();
    LocalDate maxDate = today.plusDays(14); // 확정된 2주치 범위

    List<Todo> activeTodos = todoRepository.findAllByIsActiveTrue();

    for (Todo todo : activeTodos) {
      // 랜덤 배정 멤버 조회 로직 (User ID -> User 객체 변환)
      List<User> roomMembers = Collections.emptyList();
      if (Boolean.TRUE.equals(todo.getIsRandom())) {
        List<Long> memberIds = roomMemberRepository.findByRoomIdAndLeftAtIsNullOrderByJoinedAtAsc(todo.getRoom().getId())
            .stream()
            .map(RoomMember::getUserId)
            .collect(Collectors.toList());
        roomMembers = userRepository.findAllById(memberIds);
      }

      for (LocalDate targetDate = today; !targetDate.isAfter(maxDate); targetDate = targetDate.plusDays(1)) {
        if (isInvalidDate(todo, targetDate)) continue;

        if (isRecurringDay(todo, targetDate)) {
          if (!todoInstanceRepository.existsByTodoIdAndTargetDate(todo.getId(), targetDate)) {
            User assignee = determineAssignee(todo, roomMembers);
            saveInstance(todo, todo.getRoom(), assignee, targetDate);
          }
        }
      }
    }
  }

  // --- 헬퍼 메서드 ---

  private void saveInstance(Todo todo, Room room, User assignee, LocalDate date) {
    TodoInstance instance = TodoInstance.builder()
        .todo(todo).room(room).actualAssignee(assignee)
        // 인스턴스 엔티티에 추가된 title, memo 필드에 마스터 값 복사
        .title(todo.getTitle()).memo(todo.getMemo())
        .targetDate(date).status(TodoStatus.PENDING)
        .build();
    todoInstanceRepository.save(instance);
  }

  private boolean isInvalidDate(Todo todo, LocalDate date) {
    return date.isBefore(todo.getStartDate()) ||
        (todo.getEndDate() != null && date.isAfter(todo.getEndDate()));
  }

  private boolean isRecurringDay(Todo todo, LocalDate targetDate) {
    RecurringType type = todo.getRecurringType();

    if (type == RecurringType.WEEKLY) {
      int dayOfWeek = targetDate.getDayOfWeek().getValue();
      return todo.getRepeatDays() != null && todo.getRepeatDays().contains(String.valueOf(dayOfWeek));
    }

    if (type == RecurringType.MONTHLY) {
      return todo.getStartDate().getDayOfMonth() == targetDate.getDayOfMonth();
    }

    return false;
  }

  private User determineAssignee(Todo todo, List<User> members) {
    if (Boolean.TRUE.equals(todo.getIsRandom()) && members != null && !members.isEmpty()) {
      int randomIndex = new Random().nextInt(members.size());
      return members.get(randomIndex);
    }
    return todo.getDefaultAssignee();
  }

  private Slice<TodoInstance> fetchMissedTodos(Long userId, LocalDate today, String cursor, Pageable pageable) {
    LocalDate cursorDate;
    Long cursorId;

    if (cursor == null || cursor.isBlank()) {
      // MySQL DATE 범위를 넘지 않도록 9999년으로 설정
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
    Long cursorId = 0L;
    if (cursor != null && !cursor.isBlank()) {
      String[] parts = cursor.split("_");
      cursorDate = LocalDate.parse(parts[0]);
      cursorId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findUpcomingTodosWithCursor(userId, today, cursorDate, cursorId, pageable);
  }
}