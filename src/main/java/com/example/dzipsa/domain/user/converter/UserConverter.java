package com.example.dzipsa.domain.user.converter;

import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;

import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .providerType(user.getProviderType())
                .profileImageUrl(user.getProfileImageUrl())
                .terms_agreed(user.isTerms_agreed())
                .role(user.getRole())
                .build();
    }
}
