package com.dev.IbioScience.dto.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dev.IbioScience.enums.settlement.SettlementOrderInclusionStatus;

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
public class SettlementOrderModalDto {

    private Long settlementOrderId;
    private Long settlementId;

    private Long orderId;
    private String orderNo;
    private String ordererName;
    private LocalDateTime basisDate;

    /** 정산 주문 기준 수량 */
    private Integer dealerItemCount;

    /**
     * 현재 스키마상 주문 총액/수량 기준으로 계산한 평균 개당금액
     * (정확한 라인 단가 스냅샷은 아님)
     */
    private Long unitAmount;

    /** 주문 내 해당 셀러 딜러상품 총액 */
    private Long dealerItemAmount;

    /** 정산 스냅샷 수수료율 */
    private BigDecimal commissionRate;

    /** 주문 기준 수수료 */
    private Long commissionAmount;

    /** 주문 기준 정산금액 */
    private Long settlementAmount;

    /** 포함상태 */
    private SettlementOrderInclusionStatus inclusionStatus;

    /** 실제 정산 반영 여부 */
    private boolean included;

    /** 관리자 메모 */
    private String memo;
}