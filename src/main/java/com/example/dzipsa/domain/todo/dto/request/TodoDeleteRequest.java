package com.example.dzipsa.domain.todo.dto.request;

import com.example.dzipsa.domain.todo.entity.enums.DeleteScope;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodoDeleteRequest {
  private DeleteScope scope;
}
