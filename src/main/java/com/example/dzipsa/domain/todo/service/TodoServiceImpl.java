package com.example.dzipsa.domain.todo.service;

import com.example.dzipsa.domain.room.entity.Room;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
   * 지연/오늘/예정 세 영역을 한 번에 조회하여 메인 홈 화면에 전달
   */
  @Override
  public MyTodoListResponse getMyTodoList(Long userId, String missedCursor, String todayCursor, String upcomingCursor) {
    LocalDate today = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(0, 10);

    // 1. 지연된 할 일 (과거 미완료 건, 최근 날짜순)
    Slice<TodoInstance> missedSlice = fetchMissedTodos(userId, today, missedCursor, pageRequest);

    // 2. 오늘의 할 일 (오늘 날짜 건, ID 오름차순)
    Slice<TodoInstance> todaySlice = fetchTodayTodos(userId, today, todayCursor, pageRequest);

    // 3. 예정된 할 일 (내일 이후 건, 날짜 오름차순)
    Slice<TodoInstance> upcomingSlice = fetchUpcomingTodos(userId, today, upcomingCursor, pageRequest);

    return MyTodoListResponse.builder()
        .missedTodos(TodoConverter.toPagedResponse(missedSlice))
        .todayTodos(TodoConverter.toPagedResponse(todaySlice))
        .upcomingTodos(TodoConverter.toPagedResponse(upcomingSlice))
        .build();
  }

  /**
   * [나의 할 일 - 섹션별 개별 페이징 조회]
   * 무한 스크롤 시 호출되는 인터페이스 구현 메서드들
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
   * [우리 집 오늘 전체 할 일 조회]
   * 현재 사용자가 속한 방의 모든 구성원이 오늘 해야 할 일을 생성순으로 조회
   */
  @Override
  public List<TodoSummaryResponse> getRoomTodoList(Long roomId) {
    return todoInstanceRepository.findAllByRoomIdAndTargetDateOrderByCreatedAtAsc(roomId, LocalDate.now())
        .stream()
        .map(TodoConverter::toSummaryResponse)
        .collect(Collectors.toList());
  }

  /**
   * [완료된 할 일 모아보기]
   * 특정 방에서 완료(COMPLETED)된 할 일들을 최신 완료순으로 페이징 조회
   */
  @Override
  public Slice<TodoCompletedResponse> getCompletedTodos(Long roomId, int page, int size) {
    Slice<TodoInstance> completed = todoInstanceRepository.findAllByRoomIdAndStatusOrderByCreatedAtDesc(
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
        .orElseThrow(() -> new EntityNotFoundException("해당 할 일을 찾을 수 없습니다. ID: " + instanceId));

    // 1. 권한 체크 (임시)
    if (!instance.getActualAssignee().getId().equals(userId)) {
      log.warn("[Security] 할 일 담당자 불일치 - instanceId: {}, userId: {}", instanceId, userId);
    }

    String imageUrl = null; // 기본값은 null

    try {
      // 2. 이미지가 존재할 때만 S3 업로드 진행
      if (image != null && !image.isEmpty()) {
        imageUrl = s3Uploader.upload(image, "todo-proof");
      }

      // 3. DB 상태 변경
      instance.complete(imageUrl);

    } catch (IOException e) {
      log.error("할 일 완료 처리 중 S3 업로드 실패 - instanceId: {}", instanceId, e);
      throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.");
    }
  }

  /**
   * [할 일 수정 및 변경사항 전파]
   * 1. 마스터 정보 수정
   * 2. 수정 시점 이후의 미완료(PENDING) 인스턴스들에 변경된 담당자를 자동으로 업데이트
   */
  @Override
  @Transactional
  public void updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
    Todo todo = todoRepository.findById(todoId)
        .orElseThrow(() -> new RuntimeException("수정할 원본 데이터가 없습니다."));

    User newAssignee = (request.getAssigneeId() != null)
        ? userRepository.findById(request.getAssigneeId()).orElse(todo.getDefaultAssignee())
        : todo.getDefaultAssignee();

    todo.update(request.getTitle(), request.getMemo(), newAssignee,
        request.getRecurringType(), request.getRepeatDays(), request.getEndDate());

    List<TodoInstance> futureInstances = todoInstanceRepository.findAllByTodoIdAndTargetDateAfter(todoId, LocalDate.now());
    futureInstances.forEach(instance -> instance.updateActualAssignee(newAssignee));
  }

  /**
   * [반복 일정 자동 생성 배치]
   * 매일 자정 스케줄러를 통해 실행
   * 활성화된 반복 설정에 따라 오늘의 할 일을 자동 생성
   */
  @Override
  @Transactional
  public void generateRecurringTodos() {
    LocalDate today = LocalDate.now();
    int dayOfWeek = today.getDayOfWeek().getValue();

    List<Todo> activeTodos = todoRepository.findAllByIsActiveTrue();

    for (Todo todo : activeTodos) {
      if (today.isBefore(todo.getStartDate()) || (todo.getEndDate() != null && today.isAfter(todo.getEndDate()))) {
        continue;
      }

      if (isRecurringDay(todo, today, dayOfWeek)) {
        if (!todoInstanceRepository.existsByTodoIdAndTargetDate(todo.getId(), today)) {
          saveInstance(todo, todo.getRoom(), todo.getDefaultAssignee(), today);
        }
      }
    }
  }

  // --- 헬퍼 메서드 ---

  private void saveInstance(Todo todo, Room room, User assignee, LocalDate date) {
    TodoInstance instance = TodoInstance.builder()
        .todo(todo).room(room).actualAssignee(assignee)
        .targetDate(date).status(TodoStatus.PENDING)
        .build();
    todoInstanceRepository.save(instance);
  }

  private boolean isRecurringDay(Todo todo, LocalDate today, int dayOfWeek) {
    if (todo.getRecurringType() == RecurringType.WEEKLY) {
      return todo.getRepeatDays() != null && todo.getRepeatDays().contains(String.valueOf(dayOfWeek));
    } else if (todo.getRecurringType() == RecurringType.MONTHLY) {
      return todo.getStartDate().getDayOfMonth() == today.getDayOfMonth();
    }
    return false;
  }

  private Slice<TodoInstance> fetchMissedTodos(Long userId, LocalDate today, String cursor, PageRequest pr) {
    LocalDate mDate = LocalDate.MAX;
    Long mId = Long.MAX_VALUE;
    if (cursor != null && !cursor.isEmpty()) {
      String[] parts = cursor.split("_");
      mDate = LocalDate.parse(parts[0]);
      mId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findMissedTodosWithCursor(userId, today, mDate, mId, pr);
  }

  private Slice<TodoInstance> fetchTodayTodos(Long userId, LocalDate today, String cursor, PageRequest pr) {
    Long tId = 0L;
    if (cursor != null && !cursor.isEmpty()) {
      tId = Long.parseLong(cursor.split("_")[1]);
    }
    return todoInstanceRepository.findTodayTodosWithCursor(userId, today, tId, pr);
  }

  private Slice<TodoInstance> fetchUpcomingTodos(Long userId, LocalDate today, String cursor, PageRequest pr) {
    LocalDate uDate = today.plusDays(1);
    Long uId = 0L;
    if (cursor != null && !cursor.isEmpty()) {
      String[] parts = cursor.split("_");
      uDate = LocalDate.parse(parts[0]);
      uId = Long.parseLong(parts[1]);
    }
    return todoInstanceRepository.findUpcomingTodosWithCursor(userId, today, uDate, uId, pr);
  }
}