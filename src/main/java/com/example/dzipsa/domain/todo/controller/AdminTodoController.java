package com.example.dzipsa.domain.todo.controller;

import com.example.dzipsa.domain.todo.service.TodoBatchService; // 1. 임포트 추가
import com.example.dzipsa.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/todos/scheduler")
@RequiredArgsConstructor
public class AdminTodoController {

  private final TodoBatchService todoBatchService;

  /**
   * [관리자용 - 할 일 배치 수동 실행]
   * URL: POST /api/admin/todos/scheduler/execute
   */
  @PostMapping("/execute")
  public ResponseEntity<String> executeBatch(@AuthenticationPrincipal User user) {
    log.info("▶ [Manual Batch] 관리자(ID: {})에 의한 수동 배치 실행 (자동 판별)", user.getId());

    // 서비스에서 오늘이 월요일인지, 1일인지 판단해서 14일 혹은 30일치를 생성함
    String result = todoBatchService.executeSmartBatch();

    return ResponseEntity.ok(result);
  }
}