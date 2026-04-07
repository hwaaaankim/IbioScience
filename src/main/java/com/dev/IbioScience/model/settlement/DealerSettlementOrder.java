package com.dev.IbioScience.model.settlement;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.settlement.SettlementOrderInclusionStatus;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.order.Order;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    name = "tb_dealer_settlement_order",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_dealer_settlement_order", columnNames = {"settlement_id", "order_id"})
    },
    indexes = {
        @Index(name = "ix_dealer_settlement_order_settlement", columnList = "settlement_id"),
        @Index(name = "ix_dealer_settlement_order_order", columnList = "order_id"),
        @Index(name = "ix_dealer_settlement_order_inclusion_status", columnList = "inclusion_status"),
        @Index(name = "ix_dealer_settlement_order_settlement_status", columnList = "settlement_id,inclusion_status")
    }
)
public class DealerSettlementOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 정산 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private DealerSettlement settlement;

    /** 원주문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_id_snapshot", nullable = false)
    private Long orderIdSnapshot;

    @Column(name = "order_no_snapshot", nullable = false, length = 40)
    private String orderNoSnapshot;

    @Column(name = "orderer_name_snapshot", length = 100)
    private String ordererNameSnapshot;

    /** 기준일시 스냅샷 (paidAt / deliveredAt / purchaseConfirmedAt) */
    @Column(name = "basis_date_snapshot", nullable = false)
    private LocalDateTime basisDateSnapshot;

    /** 정산 포함 상태 */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "inclusion_status", nullable = false, length = 20)
    private SettlementOrderInclusionStatus inclusionStatus = SettlementOrderInclusionStatus.NORMAL;

    /** 이 주문 안에서 해당 셀러 딜러상품 원거래금액 합계 */
    @Column(name = "dealer_item_amount", nullable = false)
    private Long dealerItemAmount;

    /** 이 주문 기준 수수료 금액 */
    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    /** 이 주문 기준 정산 금액 */
    @Column(name = "settlement_amount", nullable = false)
    private Long settlementAmount;

    @Column(name = "dealer_item_count", nullable = false)
    private Integer dealerItemCount;

    /** 관리자 메모 */
    @Column(name = "memo", length = 1000)
    private String memo;

    @PrePersist
    @PreUpdate
    private void applyDefaultValues() {
        if (this.inclusionStatus == null) {
            this.inclusionStatus = SettlementOrderInclusionStatus.NORMAL;
        }

        if (this.memo != null && this.memo.isBlank()) {
            this.memo = null;
        }

        if (this.commissionAmount == null) {
            this.commissionAmount = 0L;
        }

        if (this.settlementAmount == null) {
            long gross = this.dealerItemAmount == null ? 0L : this.dealerItemAmount;
            long commission = this.commissionAmount == null ? 0L : this.commissionAmount;
            this.settlementAmount = gross - commission;
        }
    }
}