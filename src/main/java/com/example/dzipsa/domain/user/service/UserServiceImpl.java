package com.example.dzipsa.domain.user.service;

import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.service.RoomService;
import com.example.dzipsa.domain.user.converter.UserConverter;
import com.example.dzipsa.domain.user.dto.request.UpdateProfileRequest;
import com.example.dzipsa.domain.user.dto.response.ProfileImageResponse;
import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.UserErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomService roomService;

    @Override
    public UserResponse findById(Long userId) {
        log.debug("[UserServiceImpl] findById userId={}", userId);
        User user = getUserById(userId);
        
        boolean hasRoom = checkHasRoom(userId);
        
        return userConverter.toResponse(user, hasRoom);
    }

    @Override
    public UserResponse findByEmail(String email) {
        log.debug("[UserServiceImpl] findByEmail email={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[UserServiceImpl] findByEmail 실패 - 사용자 없음 email={}", email);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });
        
        boolean hasRoom = checkHasRoom(user.getId());
        
        return userConverter.toResponse(user, hasRoom);
    }

    @Override
    public ProfileImageResponse getProfileImage(Long userId) {
        log.debug("[UserServiceImpl] getProfileImage userId={}", userId);
        User user = getUserById(userId);
        return userConverter.toProfileImageResponse(user);
    }

    @Override
    @Transactional
    public void agreeTerms(Long userId) {
        log.debug("[UserServiceImpl] agreeTerms userId={}", userId);
        User user = getUserById(userId);
        
        if (user.isTermsAgreed()) {
            log.warn("[UserServiceImpl] 이미 이용약관에 동의함. userId={}", userId);
            throw new BusinessException(UserErrorCode.ALREADY_AGREED_TERMS);
        }
        
        user.agreeTerms();
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.debug("[UserServiceImpl] updateProfile userId={}, nickname={}, profileImageUrl={}",
                userId, request.getNickname(), request.getProfileImageUrl());
        User user = getUserById(userId);

        // 방 구성원과 프로필 이미지 중복 검증
        validateUniqueProfileImageInRoom(user, request.getProfileImageUrl());

        user.updateProfile(request.getNickname(), request.getProfileImageUrl());

        boolean hasRoom = checkHasRoom(userId);
        return userConverter.toResponse(user, hasRoom);
    }

    private void validateUniqueProfileImageInRoom(User user, String newProfileImageUrl) {
        if (newProfileImageUrl == null || newProfileImageUrl.isBlank() || newProfileImageUrl.equals(user.getProfileImageUrl())) {
            return;
        }

        if (checkHasRoom(user.getId())) {
            List<String> usedImages = roomService.getUsedProfileImages(user, null).getUsedImages();
            if (usedImages.contains(newProfileImageUrl)) {
                log.warn("[UserServiceImpl] 프로필 이미지 중복 - 방의 다른 구성원이 사용 중: userId={}, profileImageUrl={}",
                        user.getId(), newProfileImageUrl);
                throw new BusinessException(UserErrorCode.DUPLICATED_PROFILE_IMAGE);
            }
        }
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserServiceImpl] 사용자 조회 실패 userId={}", userId);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });
    }

    private boolean checkHasRoom(Long userId) {
        return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId).isPresent();
    }
}
