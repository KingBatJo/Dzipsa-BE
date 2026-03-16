package com.example.dzipsa.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    @Schema(description = "새 닉네임", example = "멋쟁이토마토")
    private String nickname;

    @Schema(description = "새 프로필 이미지 URL (1~6 사이의 숫자)", example = "3")
    private String profileImageUrl;

}
