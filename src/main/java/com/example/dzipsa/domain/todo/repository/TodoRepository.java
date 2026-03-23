package com.example.dzipsa.domain.todo.repository;

import com.example.dzipsa.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
  // 배치를 돌리기 위한 전체 활성 할 일 조회
  List<Todo> findAllByIsActiveTrue();
}