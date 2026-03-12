package com.example.dzipsa.domain.auth.repository;

import com.example.dzipsa.domain.auth.entity.OAuthAccount;
import com.example.dzipsa.domain.auth.entity.enums.OAuthProvider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    Optional<OAuthAccount> findByUserId(Long userId);
}
