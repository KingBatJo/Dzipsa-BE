package com.example.dzipsa.domain.auth.dto.info;

import java.util.Map;

public interface OAuth2UserInfo {

    String getProviderId();

    String getProvider();

    String getEmail();

    String getNickname();

    Map<String, Object> getAttributes();
}
