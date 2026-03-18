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
  private User actualAssignee; // 실제 수행 담당자

  @Column(nullable = false)
  private LocalDate targetDate; // 할 일 수행 예정일

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(nullable = false)
  private TodoStatus status = TodoStatus.PENDING;

  private LocalDateTime completedAt; // 완료 시각

  @Column(length = 300)
  private String imageUrl; // S3에 업로드된 인증샷 URL 저장

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // 할 일 완료 처리 (인증샷 포함)
  public void complete(String imageUrl) {
    this.status = TodoStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.imageUrl = imageUrl;
  }

  // 마스터 정보 수정 시, 아직 시작 안 한(PENDING) 미래의 데이터 담당자를 변경
  public void updateActualAssignee(User newAssignee) {
    if (this.status == TodoStatus.PENDING) {
      this.actualAssignee = newAssignee;
    }
  }
}