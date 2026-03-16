package com.example.dzipsa.domain.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String name;

    @Column(length = 100)
    private String motto;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long membersCount;

    @Column(nullable = false)
    private Long score; // 방 점수

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public Room(String name, String motto, Long ownerId) {
        this.name = name;
        this.motto = motto;
        this.ownerId = ownerId;
        this.membersCount = 1L;
        this.score = 3L; // 생성 시 기본 점수 3점
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateMotto(String motto) {
        this.motto = motto;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseMemberCount() {
        this.membersCount++;
    }

    public void decreaseMemberCount() {
        if (this.membersCount > 0) {
            this.membersCount--;
        }
    }

    public void changeOwner(Long newOwnerId) {
        this.ownerId = newOwnerId;
        this.updatedAt = LocalDateTime.now();
    }

    public void addScore(Long points) {
        this.score += points;
        this.updatedAt = LocalDateTime.now();
    }
}
