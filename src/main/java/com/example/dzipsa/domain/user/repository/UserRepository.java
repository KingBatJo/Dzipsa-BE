package com.example.dzipsa.domain.user.repository;

import com.example.dzipsa.domain.auth.entity.enums.ProviderType;
import com.example.dzipsa.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // 재가입 확인용: 이메일과 ProviderType이 모두 일치하는 사용자 조회
    Optional<User> findByEmailAndProviderType(String email, ProviderType providerType);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
