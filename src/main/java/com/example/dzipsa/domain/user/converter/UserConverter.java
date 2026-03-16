package com.example.dzipsa.domain.user.converter;

import com.example.dzipsa.domain.user.dto.response.ProfileImageResponse;
import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;

import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserResponse toResponse(User user, boolean hasRoom) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .providerType(user.getProviderType())
                .profileImageUrl(user.getProfileImageUrl())
                .termsAgreed(user.isTermsAgreed())
                .role(user.getRole())
                .hasRoom(hasRoom)
                .build();
    }

    public ProfileImageResponse toProfileImageResponse(User user) {
        return ProfileImageResponse.builder()
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
