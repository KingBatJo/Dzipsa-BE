package com.example.dzipsa.domain.todo.entity;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.todo.entity.enums.TodoStatus;
import com.example.dzipsa.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * [Todo 실행 인스턴스 엔티티]
 * Todo 마스터를 바탕으로 실제 특정 날짜에 수행해야 할 할 일 기록
 * 유저의 리스트에 노출되는 실제 데이터 단위
 */
@Entity
@Table(name = "todo_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TodoInstance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "todo_id", nullable = false)
  private Todo todo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actual_assignee_id")
  private User actualAssignee;

  @Column(nullable = false, length = 30)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Column(nullable = false)
  private LocalDate targetDate;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(nullable = false)
  private TodoStatus status = TodoStatus.PENDING;

  private LocalDateTime completedAt;

  @Column(length = 300)
  private String imageUrl;

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // 비즈니스 로직: 할 일 완료 처리
  public void complete(String imageUrl) {
    this.status = TodoStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.imageUrl = imageUrl;
  }

  // 마스터 정보 변경 시 미래 인스턴스 일괄 업데이트 메서드
  public void updateFromMaster(String title, String memo, User newAssignee) {
    if (this.status == TodoStatus.PENDING) {
      this.title = title;
      this.memo = memo;
      this.actualAssignee = newAssignee;
    }
  }

  public void removeImage() {
    this.imageUrl = null;
  }
}