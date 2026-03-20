package com.dev.IbioScience.model.product.coupon;

import com.dev.IbioScience.enums.product.coupon.MemberCouponHistoryActionType;
import com.dev.IbioScience.enums.product.coupon.MemberCouponHistorySourceType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.relation.MemberCoupon;

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
    name = "tb_member_coupon_history",
    indexes = {
        @Index(name = "ix_member_coupon_history_member_coupon_id", columnList = "member_coupon_id"),
        @Index(name = "ix_member_coupon_history_member_id", columnList = "member_id"),
        @Index(name = "ix_member_coupon_history_coupon_id", columnList = "coupon_id"),
        @Index(name = "ix_member_coupon_history_order_id", columnList = "order_id"),
        @Index(name = "ix_member_coupon_history_created_at", columnList = "created_at")
    }
)
public class MemberCouponHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 회원쿠폰 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_coupon_id", nullable = false)
    private MemberCoupon memberCoupon;

    /** 대상 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 쿠폰 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /** 관련 주문(없을 수 있음) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** 발급 / 사용 / 삭제 */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private MemberCouponHistoryActionType actionType;

    /** 관리자 / 주문프로모션 / 주문사용 */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private MemberCouponHistorySourceType sourceType;

    /** 관리자 username */
    @Column(name = "admin_username", length = 100)
    private String adminUsername;

    /** 비고 */
    @Column(name = "description", length = 255)
    private String description;
}