package com.example.dzipsa.domain.user.controller;

import com.example.dzipsa.domain.user.dto.request.UpdateProfileRequest;
import com.example.dzipsa.domain.user.dto.response.ProfileImageResponse;
import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me/profile-image")
    @Operation(summary = "내 프로필 사진 조회", description = "로그인한 사용자의 프로필 이미지 URL을 조회합니다.")
    public ResponseEntity<ProfileImageResponse> getMyProfileImage(
            @AuthenticationPrincipal User user) {
        ProfileImageResponse response = userService.getProfileImage(user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/terms-agreement")
    @Operation(summary = "이용약관 동의", description = "로그인한 사용자의 이용약관 동의 여부를 true로 변경합니다.")
    public ResponseEntity<Void> agreeTerms(
            @AuthenticationPrincipal User user) {
        userService.agreeTerms(user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "로그인한 사용자의 닉네임과 프로필 이미지를 수정합니다.")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User user) {
        UserResponse response = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(response);
    }

}
