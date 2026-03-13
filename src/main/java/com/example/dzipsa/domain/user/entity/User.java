package com.example.dzipsa.domain.user.entity;

import com.example.dzipsa.domain.auth.entity.enums.ProviderType;
import com.example.dzipsa.domain.user.entity.enums.UserRole;
import com.example.dzipsa.domain.user.entity.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true 제거됨
    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private String profileImageUrl;

    private boolean terms_agreed;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProviderType providerType;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String nickname, UserStatus status, UserRole role,
        ProviderType providerType) {
        this.email = email;
        this.nickname = nickname;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.role = role != null ? role : UserRole.USER;
        this.providerType = providerType;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
        this.updatedAt = LocalDateTime.now();
    }


    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void rejoin(String nickname) {
        this.status = UserStatus.ACTIVE;
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }
}
