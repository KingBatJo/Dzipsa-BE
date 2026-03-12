package com.example.dzipsa.domain.user.service;

import com.example.dzipsa.domain.user.converter.UserConverter;
import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.UserErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;

    public UserResponse findById(Long userId) {
        log.debug("[UserService] findById userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserService] findById 실패 - 사용자 없음 userId={}", userId);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });
        return userConverter.toResponse(user);
    }

    public UserResponse findByEmail(String email) {
        log.debug("[UserService] findByEmail email={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[UserService] findByEmail 실패 - 사용자 없음 email={}", email);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });
        return userConverter.toResponse(user);
    }
}
