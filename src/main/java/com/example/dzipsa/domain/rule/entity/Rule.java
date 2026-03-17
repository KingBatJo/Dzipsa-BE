package com.example.dzipsa.domain.rule.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long roomId;

    @NotNull
    @Column(nullable = false)
    private Long registerId;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String title;

    @Size(max = 300)
    @Column(length = 300)
    private String memo;

    @Column(nullable = false)
    private boolean timeSettingEnabled;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false)
    private boolean repeatEnabled;

    @Size(max = 50)
    @Column(length = 50)
    private String repeatDays; // 1,2... (1:월, 7:일)

    @Column(nullable = false)
    private boolean notiEnabled;

    @NotNull
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Builder
    public Rule(Long roomId, Long registerId, String title, String memo,
                boolean timeSettingEnabled, LocalTime startTime, LocalTime endTime,
                boolean repeatEnabled, String repeatDays, boolean notiEnabled) {
        this.roomId = roomId;
        this.registerId = registerId;
        this.title = title;
        this.memo = memo;
        this.timeSettingEnabled = timeSettingEnabled;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatEnabled = repeatEnabled;
        this.repeatDays = repeatDays;
        this.notiEnabled = notiEnabled;
    }

    public void update(String title, String memo, boolean timeSettingEnabled, LocalTime startTime, LocalTime endTime,
                       boolean repeatEnabled, String repeatDays, boolean notiEnabled) {
        this.title = title;
        this.memo = memo;
        this.timeSettingEnabled = timeSettingEnabled;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatEnabled = repeatEnabled;
        this.repeatDays = repeatDays;
        this.notiEnabled = notiEnabled;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
