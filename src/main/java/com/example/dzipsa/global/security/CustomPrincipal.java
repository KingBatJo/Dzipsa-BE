package com.example.dzipsa.global.security;

import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.entity.enums.UserRole;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import lombok.Getter;

@Getter
public class CustomPrincipal implements OAuth2User {

    private final Long userId;
    private final String email;
    private final UserRole role;
    private final Map<String, Object> attributes;
    private final boolean isNewUser;

    public CustomPrincipal(User user, Map<String, Object> attributes, boolean isNewUser) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole(); // 트랜잭션 안에서 값을 미리 꺼내 저장 (Lazy Loading 방지)
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
        this.isNewUser = isNewUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getName() {
        return email;
    }
}
