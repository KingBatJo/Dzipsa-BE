package com.example.dzipsa.domain.user.service;

import com.example.dzipsa.domain.user.dto.request.UpdateProfileRequest;
import com.example.dzipsa.domain.user.dto.response.ProfileImageResponse;
import com.example.dzipsa.domain.user.dto.response.UserResponse;

public interface UserService {
    UserResponse findById(Long userId);
    UserResponse findByEmail(String email);
    ProfileImageResponse getProfileImage(Long userId);
    void agreeTerms(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
}
