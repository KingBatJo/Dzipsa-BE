package com.example.dzipsa.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {

    @Schema(description = "프로필 이미지 URL (1~6 사이의 숫자)", example = "3")
    private String profileImageUrl;

}
