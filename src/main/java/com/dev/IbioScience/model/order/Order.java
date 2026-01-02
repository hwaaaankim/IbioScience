package com.dev.IbioScience.model.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.product.relation.MemberCoupon;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "tb_order",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_order_no", columnNames = {"order_no"})
    },
    indexes = {
        @Index(name = "ix_order_member_id", columnList = "member_id"),
        @Index(name = "ix_order_status", columnList = "status"),
        @Index(name = "ix_order_created_at", columnList = "created_at")
    }
)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문번호(화면 표시/조회용) */
    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    /** 주문자(로그인 회원) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 주문상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    /** 결제수단 */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /** 결제 승인/완료 일시(테스트 팝업에서 '결제완료' 눌렀을 때 세팅) */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 배송방법 */
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 30)
    private ShippingMethod shippingMethod;

    /** 선불/착불 */
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_pay_type", nullable = false, length = 30)
    private ShippingPayType shippingPayType;

    // =========================
    // 배송지 스냅샷(주문서 입력값)
    // =========================
    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    /** 휴대폰 분리 3칸(필수) */
    @Column(name = "hp1", nullable = false, length = 3)
    private String hp1;

    @Column(name = "hp2", nullable = false, length = 4)
    private String hp2;

    @Column(name = "hp3", nullable = false, length = 4)
    private String hp3;

    /** 유선전화(선택) */
    @Column(name = "tel1", length = 3)
    private String tel1;

    @Column(name = "tel2", length = 4)
    private String tel2;

    @Column(name = "tel3", length = 4)
    private String tel3;

    @Column(name = "postcode", nullable = false, length = 20)
    private String postcode;

    @Column(name = "road_address", nullable = false, length = 300)
    private String roadAddress;

    @Column(name = "detail_address", nullable = false, length = 300)
    private String detailAddress;

    @Column(name = "shipping_memo", length = 300)
    private String shippingMemo;

    // =========================
    // 주문금액 스냅샷(서버 계산 결과)
    // =========================
    /** 상품합계(옵션라인 합) */
    @Column(name = "sum_price", nullable = false)
    private Long sumPrice;

    /** 배송비(선불/착불 무관하게 최종금액에 포함 정책) */
    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee;

    /** 기본 프로모션 할인(현재 0, 확장용) */
    @Column(name = "base_discount", nullable = false)
    private Long baseDiscount;

    /** 쿠폰할인(금액형) */
    @Column(name = "coupon_discount", nullable = false)
    private Long couponDiscount;

    /** 사용 적립금 */
    @Column(name = "point_used", nullable = false)
    private Long pointUsed;

    /** 최종 결제금액 */
    @Column(name = "grand_total", nullable = false)
    private Long grandTotal;

    /** 예상 적립 포인트(5%) */
    @Column(name = "expect_point", nullable = false)
    private Long expectPoint;

    // =========================
    // 쿠폰 적용(선택)
    // =========================
    /** 사용한 회원쿠폰(선택) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_coupon_id")
    private MemberCoupon memberCoupon;

    /** 쿠폰 스냅샷(선택) */
    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    @Column(name = "coupon_name", length = 200)
    private String couponName;

    // =========================
    // 주문자 정보(표시용 스냅샷)
    // =========================
    @Column(name = "orderer_name", length = 100)
    private String ordererName;

    @Column(name = "orderer_phone", length = 30)
    private String ordererPhone;

    @Column(name = "order_sms_agree", nullable = false)
    private Boolean orderSmsAgree;

    // =========================
    // Items
    // =========================
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        if (item == null) return;
        item.setOrder(this);
        this.items.add(item);
    }
}