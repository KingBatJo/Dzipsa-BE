package com.example.dzipsa.domain.todo.entity.enums;

public enum DeleteScope {
  ONLY_THIS,       // 이 할 일만 삭제
  SINCE_THIS,      // 이후 할 일 모두 삭제
  ALL_RECURRING    // 전체 반복 할 일 삭제
}