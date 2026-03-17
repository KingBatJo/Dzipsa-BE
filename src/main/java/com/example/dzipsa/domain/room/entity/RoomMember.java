package com.example.dzipsa.domain.room.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_members", indexes = {
    @Index(name = "idx_room_user_joined", columnList = "roomId, userId, joinedAt", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long roomId;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Builder
    public RoomMember(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }
}
