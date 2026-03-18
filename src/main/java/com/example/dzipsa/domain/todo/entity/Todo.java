package com.example.dzipsa.domain.todo.entity;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.todo.entity.enums.RecurringType;
import com.example.dzipsa.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * [Todo 마스터 엔티티]
 * 할 일의 원형을 저장
 * 실제 실행은 TodoInstance에서 담당
 */
@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Todo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room; // 어떤 방의 할 일인지

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "writer_id", nullable = false)
  private User writer; // 작성자

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "default_assignee_id")
  private User defaultAssignee; // 기본 담당자

  @Column(nullable = false, length = 30)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isRandom = false;

  @Column(nullable = false)
  private LocalDate startDate;

  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(nullable = false)
  private RecurringType recurringType = RecurringType.NONE;

  @Column(name = "repeat_days")
  private String repeatDays;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isActive = true;

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  // 비즈니스 로직: 할 일 정보 수정 메서드
  public void update(String title, String memo, User defaultAssignee,
      RecurringType recurringType, String repeatDays, LocalDate endDate) {
    this.title = title;
    this.memo = memo;
    this.defaultAssignee = defaultAssignee;
    this.recurringType = recurringType;
    this.repeatDays = repeatDays;
    this.endDate = endDate;
  }
  // 비즈니스 로직: 할 일 정보 삭제 메서드
  public void delete() {
    this.isActive = false;
    this.deletedAt = LocalDateTime.now();
  }
}