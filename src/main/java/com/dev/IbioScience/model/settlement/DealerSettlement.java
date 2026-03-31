package com.dev.IbioScience.model.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.settlement.SettlementPayStatus;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tb_dealer_settlement",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_dealer_settlement_unique_period",
            columnNames = {"seller_dealer_profile_id", "period_start_date", "period_end_date", "settlement_basis"}
        )
    },
    indexes = {
        @Index(name = "ix_dealer_settlement_seller", columnList = "seller_dealer_profile_id"),
        @Index(name = "ix_dealer_settlement_period_start", columnList = "period_start_date"),
        @Index(name = "ix_dealer_settlement_period_end", columnList = "period_end_date"),
        @Index(name = "ix_dealer_settlement_pay_status", columnList = "pay_status"),
        @Index(name = "ix_dealer_settlement_executed_at", columnList = "executed_at")
    }
)
public class DealerSettlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 셀러 원본 참조 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_dealer_profile_id", nullable = false)
    private SellerDealerProfile sellerDealerProfile;

    /** 어떤 정책 이력으로 계산되었는지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_history_id", nullable = false)
    private DealerSettlementPolicyHistory policyHistory;

    /** 어떤 실행 배치에서 만들어졌는지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private DealerSettlementBatch batch;

    /** 정산 대상 기간 */
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    /** 정산정책 스냅샷 */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", nullable = false, length = 20)
    private SettlementCycle settlementCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_basis", nullable = false, length = 30)
    private SettlementBasis settlementBasis;

    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate;

    /** 금액 */
    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    @Column(name = "settlement_amount", nullable = false)
    private Long settlementAmount;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    /** 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", nullable = false, length = 20)
    private SettlementPayStatus payStatus;

    /** 정산 생성 실행일시 */
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    /** 실제 지급 처리일시 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 스냅샷 */
    @Column(name = "seller_member_id_snapshot")
    private Long sellerMemberIdSnapshot;

    @Column(name = "member_username_snapshot", length = 100)
    private String memberUsernameSnapshot;

    @Column(name = "member_name_snapshot", length = 100)
    private String memberNameSnapshot;

    @Column(name = "member_email_snapshot", length = 200)
    private String memberEmailSnapshot;

    @Column(name = "member_mobile_snapshot", length = 30)
    private String memberMobileSnapshot;

    @Column(name = "company_name_snapshot", length = 200)
    private String companyNameSnapshot;

    @Column(name = "shop_name_snapshot", length = 200)
    private String shopNameSnapshot;

    @Column(name = "supplier_code_snapshot", length = 40)
    private String supplierCodeSnapshot;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DealerSettlementOrder> orders = new ArrayList<>();

    public void addOrder(DealerSettlementOrder order) {
        if (order == null) {
            return;
        }
        order.setSettlement(this);
        this.orders.add(order);
    }
}