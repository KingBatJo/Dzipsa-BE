package com.example.dzipsa.domain.rule.service;

import com.example.dzipsa.domain.rule.dto.request.RuleCreateRequest;
import com.example.dzipsa.domain.rule.dto.request.RuleUpdateRequest;
import com.example.dzipsa.domain.rule.dto.response.RuleListResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleWarningResponse;
import com.example.dzipsa.domain.user.entity.User;

import java.util.List;

public interface RuleService {
    RuleResponse createRule(RuleCreateRequest request, User user);
    RuleResponse updateRule(Long ruleId, RuleUpdateRequest request, User user);
    RuleResponse getRule(Long ruleId, User user);
    List<RuleListResponse> getRules(Long cursor, int size, User user);
    void deleteRule(Long ruleId, User user);
    
    RuleWarningResponse createWarning(Long ruleId, User user);
    List<RuleWarningResponse> getRecentWarnings(User user);
}
