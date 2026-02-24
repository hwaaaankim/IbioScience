package com.dev.IbioScience.service.logging;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberAuditLog;
import com.dev.IbioScience.repository.auth.MemberAuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberAuditLogService {

    private final MemberAuditLogRepository memberAuditLogRepository;

    @Transactional
    public void logEvent(Member target, MemberAuditAction action, String newValue, Long actorMemberId) {
        if (target == null || action == null) return;

        MemberAuditLog log = MemberAuditLog.builder()
            .member(target)
            .action(action)
            .fieldName(null)
            .oldValue(null)
            .newValue(limit(newValue))
            .actorMemberId(actorMemberId)
            .build();

        memberAuditLogRepository.save(log);
    }

    @Transactional
    public void logFieldChange(Member target, MemberAuditAction action,
                               String fieldName, String oldValue, String newValue,
                               Long actorMemberId) {
        if (target == null || action == null) return;

        // 값이 실제로 바뀐 경우만 기록(과다로그 방지)
        if (Objects.equals(nvl(oldValue), nvl(newValue))) return;

        MemberAuditLog log = MemberAuditLog.builder()
            .member(target)
            .action(action)
            .fieldName(limit(fieldName))
            .oldValue(limit(oldValue))
            .newValue(limit(newValue))
            .actorMemberId(actorMemberId)
            .build();

        memberAuditLogRepository.save(log);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String limit(String s) {
        if (s == null) return null;
        if (s.length() <= 1000) return s;
        return s.substring(0, 1000);
    }
}