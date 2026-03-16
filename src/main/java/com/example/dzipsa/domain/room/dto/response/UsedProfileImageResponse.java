package com.example.dzipsa.domain.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class UsedProfileImageResponse {
    private List<String> usedImages;
}
