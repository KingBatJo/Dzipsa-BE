package com.example.dzipsa.global.exception.domain;

import com.example.dzipsa.global.exception.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TodoErrorCode implements ApiCode {
  // --- 4xx Client Errors ---

  // 400 Bad Request
  INVALID_RECURRING_PARS(HttpStatus.BAD_REQUEST.value(), 540001, "반복 설정 파라미터가 유효하지 않습니다."),
  INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST.value(), 540002, "종료일은 시작일보다 빠를 수 없습니다."),

  // 403 Forbidden
  FORBIDDEN_UPDATE_LIMIT(HttpStatus.FORBIDDEN.value(), 540301, "본인에게 할당된 할 일만 상태를 변경하거나 완료할 수 있습니다."),
  FORBIDDEN_IMAGE_DELETE(HttpStatus.FORBIDDEN.value(), 540302, "본인이 등록한 인증샷만 삭제할 수 있습니다."),

  // 404 Not Found
  TODO_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 540401, "해당 할 일 마스터 정보를 찾을 수 없습니다."),
  TODO_INSTANCE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 540402, "해당 날짜의 할 일 일정을 찾을 수 없습니다."),
  ASSIGNEE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 540403, "지정된 담당자를 찾을 수 없습니다."),

  // --- 5xx Server Errors ---

  // 500 Internal Server Error
  IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), 550001, "인증샷 업로드 중 서버 오류가 발생했습니다.");


  private final Integer httpStatus;
  private final Integer code;
  private final String message;
}