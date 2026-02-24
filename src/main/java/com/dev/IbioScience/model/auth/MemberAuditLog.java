package com.dev.IbioScience.model.auth;

import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "member_audit_log",
    indexes = {
        @Index(name = "ix_audit_member", columnList = "member_id"),
        @Index(name = "ix_audit_created_at", columnList = "createdAt")
    }
)
public class MemberAuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 멤버 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * ✅ 액션(ENUM)
     * - DB 컬럼명은 기존과 동일하게 action 사용(마이그레이션 최소화)
     * - 기존 데이터(JOIN/UPDATE 등) 호환을 위해 enum에 레거시 값 포함
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private MemberAuditAction action;

    /** 변경 필드명 (가입/탈퇴 등 이벤트성 로그는 null 가능) */
    @Column(length = 100)
    private String fieldName;

    /** 이전값/이후값 (간단 텍스트) */
    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;

    /** 수행자(관리자) ID (선택) */
    private Long actorMemberId;

    /* ===== 편의 ===== */

    @Transient
    public String getActionLabelKr() {
        return action != null ? action.getLabelKr() : "-";
    }
}