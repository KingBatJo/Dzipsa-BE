package com.example.dzipsa.domain.todo.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoDetailResponse {
  private Long instanceId;
  private String title;
  private LocalDate targetDate; // 마감일
  private String memo;

  // 담당자 정보
  private Long assigneeId;
  private String assigneeNickname;
  private String assigneeProfileImage;

  // 반복 및 상태
  private String recurringInfo;     // 예: "매주 월, 목"
  private String status;            // 진행, 완료, 지연, 지연완료
  private String statusDetail;      // 예: "n일 지연", "2026.3.14 (토)" 등 추가 문구

  // 인증샷
  private String imageUrl;          // 완료 시 등록된 사진 URL

  // 권한 제어 (본인 것인지 확인용)
  private boolean isOwner;
}