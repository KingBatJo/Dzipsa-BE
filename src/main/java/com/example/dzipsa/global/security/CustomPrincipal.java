package com.example.dzipsa.global.security;

import com.example.dzipsa.domain.user.entity.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import lombok.Getter;

@Getter
public class CustomPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final boolean isNewUser; // 신규 가입 여부 추가

    public CustomPrincipal(User user, Map<String, Object> attributes, boolean isNewUser) {
        this.user = user;
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
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
