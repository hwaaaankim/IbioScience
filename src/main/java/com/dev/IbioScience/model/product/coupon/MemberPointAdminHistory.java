package com.dev.IbioScience.model.product.coupon;

import com.dev.IbioScience.enums.product.coupon.MemberPointAdminActionType;
import com.dev.IbioScience.model.auth.Member;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "tb_member_point_admin_history",
    indexes = {
        @Index(name = "ix_member_point_admin_history_member_id", columnList = "member_id"),
        @Index(name = "ix_member_point_admin_history_created_at", columnList = "created_at")
    }
)
public class MemberPointAdminHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 관리자 부여 / 차감 */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private MemberPointAdminActionType actionType;

    /** 변동 금액(양수 저장) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 처리 전 잔액 */
    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    /** 처리 후 잔액 */
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    /** 처리 관리자 username */
    @Column(name = "admin_username", nullable = false, length = 100)
    private String adminUsername;

    /** 비고 */
    @Column(name = "description", length = 255)
    private String description;
}