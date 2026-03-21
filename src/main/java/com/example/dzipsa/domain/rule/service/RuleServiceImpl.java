package com.example.dzipsa.domain.rule.service;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.rule.converter.RuleConverter;
import com.example.dzipsa.domain.rule.dto.request.RuleCreateRequest;
import com.example.dzipsa.domain.rule.dto.request.RuleUpdateRequest;
import com.example.dzipsa.domain.rule.dto.response.RuleListResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleResponse;
import com.example.dzipsa.domain.rule.dto.response.RuleWarningResponse;
import com.example.dzipsa.domain.rule.entity.Rule;
import com.example.dzipsa.domain.rule.entity.RuleWarning;
import com.example.dzipsa.domain.rule.repository.RuleRepository;
import com.example.dzipsa.domain.rule.repository.RuleWarningRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.global.exception.BusinessException;
import com.example.dzipsa.global.exception.domain.RoomErrorCode;
import com.example.dzipsa.global.exception.domain.RuleErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final RuleWarningRepository ruleWarningRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final RuleConverter ruleConverter;

    @Override
    @Transactional
    public RuleResponse createRule(RuleCreateRequest request, User user) {
        log.info("[RuleService] 규칙 생성 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());

        if (request.isNotiEnabled() && !request.isTimeSettingEnabled()) {
            throw new BusinessException(RuleErrorCode.NOTI_NOT_AVAILABLE_WITHOUT_TIME);
        }

        Rule rule = ruleConverter.toRule(request, myMember.getRoomId(), user.getId());
        ruleRepository.save(rule);

        return ruleConverter.toRuleResponse(rule, false, 0);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(Long ruleId, RuleUpdateRequest request, User user) {
        log.info("[RuleService] 규칙 수정 요청. ruleId={}, userId={}", ruleId, user.getId());
        Rule rule = findRuleById(ruleId);

        if (request.isNotiEnabled() && !request.isTimeSettingEnabled()) {
            throw new BusinessException(RuleErrorCode.NOTI_NOT_AVAILABLE_WITHOUT_TIME);
        }

        String repeatDays = request.isRepeatEnabled() ? ruleConverter.normalizeRepeatDays(request.getRepeatDays()) : null;

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (request.isTimeSettingEnabled()) {
            if (startTime == null) startTime = LocalTime.of(9, 0);
            if (endTime == null) endTime = LocalTime.of(0, 0);
        } else {
            startTime = null;
            endTime = null;
        }

        rule.update(
                request.getTitle(),
                request.getMemo(),
                request.isTimeSettingEnabled(),
                startTime,
                endTime,
                request.isRepeatEnabled(),
                repeatDays,
                request.isNotiEnabled()
        );

        int totalWarningCount = ruleWarningRepository.countByRuleId(rule.getId());
        return ruleConverter.toRuleResponse(rule, isWarningDisabled(rule.getId()), totalWarningCount);
    }

    @Override
    public RuleResponse getRule(Long ruleId, User user) {
        log.info("[RuleService] 규칙 단건 조회 요청. ruleId={}, userId={}", ruleId, user.getId());
        Rule rule = findRuleById(ruleId);
        RoomMember myMember = getActiveRoomMember(user.getId());

        if (!rule.getRoomId().equals(myMember.getRoomId())) {
            throw new BusinessException(RoomErrorCode.NOT_ROOM_MEMBER);
        }

        int totalWarningCount = ruleWarningRepository.countByRuleId(rule.getId());
        return ruleConverter.toRuleResponse(rule, isWarningDisabled(rule.getId()), totalWarningCount);
    }

    @Override
    public List<RuleListResponse> getRules(Long cursor, int size, User user) {
        log.info("[RuleService] 규칙 목록 조회 요청. userId={}, cursor={}, size={}", user.getId(), cursor, size);
        RoomMember myMember = getActiveRoomMember(user.getId());
        
        List<Rule> rules = ruleRepository.findByRoomIdWithCursor(myMember.getRoomId(), cursor, PageRequest.of(0, size));
        
        return rules.stream()
                .map(rule -> ruleConverter.toRuleListResponse(rule, isWarningDisabled(rule.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId, User user) {
        log.info("[RuleService] 규칙 삭제 요청. ruleId={}, userId={}", ruleId, user.getId());
        Rule rule = findRuleById(ruleId);

        rule.delete();
    }

    @Override
    @Transactional
    public RuleWarningResponse createWarning(Long ruleId, User user) {
        log.info("[RuleService] 집사에게 알리기(경고) 등록 요청. ruleId={}, userId={}", ruleId, user.getId());
        Rule rule = findRuleById(ruleId);
        RoomMember myMember = getActiveRoomMember(user.getId());
        
        if (!rule.getRoomId().equals(myMember.getRoomId())) {
             throw new BusinessException(RoomErrorCode.NOT_ROOM_MEMBER);
        }

        if (isWarningDisabled(ruleId)) {
            throw new BusinessException(RuleErrorCode.ALREADY_WARNED);
        }

        RuleWarning warning = RuleWarning.builder()
                .roomId(rule.getRoomId())
                .ruleId(rule.getId())
                .build();
        ruleWarningRepository.save(warning);

        // 방 점수 감소 (-0.5점)
        Room room = roomRepository.findByIdAndDeletedAtIsNull(rule.getRoomId())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
        room.decreaseScore(0.5);

        return ruleConverter.toRuleWarningResponse(warning, rule.getTitle());
    }

    @Override
    public List<RuleWarningResponse> getRecentWarnings(User user) {
        log.info("[RuleService] 최근 알리기 목록 조회 요청. userId={}", user.getId());
        RoomMember myMember = getActiveRoomMember(user.getId());
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<RuleWarning> warnings = ruleWarningRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(
                myMember.getRoomId(), last24Hours, PageRequest.of(0, 3));

        return warnings.stream()
                .map(w -> {
                    Rule rule = ruleRepository.findByIdAndDeletedAtIsNull(w.getRuleId()).orElse(null);
                    String title = (rule != null) ? rule.getTitle() : "삭제된 규칙";
                    return ruleConverter.toRuleWarningResponse(w, title);
                })
                .filter(response -> !response.getRuleTitle().equals("삭제된 규칙"))
                .collect(Collectors.toList());
    }

    private Rule findRuleById(Long ruleId) {
        return ruleRepository.findByIdAndDeletedAtIsNull(ruleId)
                .orElseThrow(() -> new BusinessException(RuleErrorCode.RULE_NOT_FOUND));
    }

    private RoomMember getActiveRoomMember(Long userId) {
        return roomMemberRepository.findByUserIdAndLeftAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
    }

    private void validateRoomOwner(Long roomId, Long userId) {
        roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .ifPresent(room -> {
                    if (!room.getOwnerId().equals(userId)) {
                        throw new BusinessException(RuleErrorCode.FORBIDDEN_NOT_OWNER);
                    }
                });
    }

    private boolean isWarningDisabled(Long ruleId) {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        return ruleWarningRepository.existsByRuleIdAndCreatedAtAfter(ruleId, last24Hours);
    }
}
