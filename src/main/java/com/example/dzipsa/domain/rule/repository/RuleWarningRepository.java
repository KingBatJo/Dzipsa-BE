package com.example.dzipsa.domain.rule.repository;

import com.example.dzipsa.domain.rule.entity.RuleWarning;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RuleWarningRepository extends JpaRepository<RuleWarning, Long> {
    
    boolean existsByRuleIdAndCreatedAtAfter(Long ruleId, LocalDateTime createdAt);
    
    List<RuleWarning> findByRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(Long roomId, LocalDateTime createdAt, Pageable pageable);

    Optional<RuleWarning> findFirstByRuleIdOrderByCreatedAtDesc(Long ruleId);

    int countByRoomIdAndCreatedAtAfter(Long roomId, LocalDateTime createdAt);

    int countByRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        Long roomId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );

    int countByRuleId(Long ruleId);
}
