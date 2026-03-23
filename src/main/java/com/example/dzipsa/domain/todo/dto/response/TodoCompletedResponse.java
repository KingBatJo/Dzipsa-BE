package com.example.dzipsa.domain.todo.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoCompletedResponse {
  private Long instanceId;
  private String title;
  private String assigneeNickname;
  private String profileImageUrl;
  private String imageUrl;
  private String completedAt;
}