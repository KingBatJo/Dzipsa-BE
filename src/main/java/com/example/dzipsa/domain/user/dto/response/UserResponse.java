package com.example.dzipsa.domain.user.dto.response;

import com.example.dzipsa.domain.auth.entity.enums.ProviderType;
import com.example.dzipsa.domain.user.entity.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private ProviderType providerType;
    private String profileImageUrl;
    private UserRole role;
}
