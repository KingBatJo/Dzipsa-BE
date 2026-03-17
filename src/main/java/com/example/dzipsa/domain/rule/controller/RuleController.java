package com.example.dzipsa.domain.rule.controller;

import com.example.dzipsa.domain.rule.dto.request.RuleCreateRequest;
import com.example.dzipsa.domain.rule.dto.request.RuleUpdateRequest;
import com.example.dzipsa.domain.rule.dto.response.RuleListResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleWarningResponse;
import com.example.dzipsa.domain.rule.service.RuleService;
import com.example.dzipsa.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Rules", description = "규칙 관련 API")
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @PostMapping
    @Operation(summary = "규칙 등록", description = "방장만 규칙을 등록할 수 있습니다.")
    public ResponseEntity<RuleResponse> createRule(
            @Valid @RequestBody RuleCreateRequest request,
            @AuthenticationPrincipal User user) {
        RuleResponse response = ruleService.createRule(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{ruleId}")
    @Operation(summary = "규칙 수정", description = "방장만 규칙을 수정할 수 있습니다.")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody RuleUpdateRequest request,
            @AuthenticationPrincipal User user) {
        RuleResponse response = ruleService.updateRule(ruleId, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "규칙 상세 조회")
    public ResponseEntity<RuleResponse> getRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal User user) {
        RuleResponse response = ruleService.getRule(ruleId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "규칙 목록 조회", description = "최신 등록순으로 정렬되며 무한 스크롤(Cursor 방식)이 적용됩니다.")
    public ResponseEntity<List<RuleListResponse>> getRules(
            @Parameter(description = "마지막으로 조회된 규칙 ID (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회할 개수 (기본값: 10)")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        List<RuleListResponse> response = ruleService.getRules(cursor, size, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "규칙 삭제", description = "방장만 규칙을 삭제할 수 있습니다.")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal User user) {
        ruleService.deleteRule(ruleId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ruleId}/warnings")
    @Operation(summary = "집사에게 알리기(경고) 등록", description = "규칙을 지키지 않았을 때 멤버가 알림을 보냅니다. 알리기 시 해당 규칙은 24시간 동안 비활성화됩니다.")
    public ResponseEntity<RuleWarningResponse> createWarning(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal User user) {
        RuleWarningResponse response = ruleService.createWarning(ruleId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/warnings/recent")
    @Operation(summary = "최근 알리기(경고) 목록 조회", description = "알리기가 생성된 지 24시간 이내인 알리기 중 최근 3개를 조회합니다.")
    public ResponseEntity<List<RuleWarningResponse>> getRecentWarnings(
            @AuthenticationPrincipal User user) {
        List<RuleWarningResponse> response = ruleService.getRecentWarnings(user);
        return ResponseEntity.ok(response);
    }
}
