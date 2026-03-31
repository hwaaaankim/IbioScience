package com.dev.IbioScience.model.settlement;

import java.time.LocalDateTime;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.order.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        @Index(name = "ix_dealer_settlement_order_order", columnList = "order_id")
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

    /** 이 주문 안에서 해당 셀러 딜러상품 합계 */
    @Column(name = "dealer_item_amount", nullable = false)
    private Long dealerItemAmount;

    @Column(name = "dealer_item_count", nullable = false)
    private Integer dealerItemCount;
}