package com.dev.IbioScience.dto.seller.settlement;

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
public class SellerSettlementManagerOrderRowDto {

    private Long orderIdSnapshot;
    private String orderNoSnapshot;
    private String ordererNameSnapshot;
    private LocalDateTime basisDateSnapshot;

    /** 관리자 정산 주문 포함 상태 */
    private SettlementOrderInclusionStatus inclusionStatus;

    /** 해당 주문 내 딜러상품 총액 */
    private Long dealerItemAmount;

    /** 해당 주문 기준 수수료 */
    private Long commissionAmount;

    /** 해당 주문 기준 실정산액 */
    private Long settlementAmount;

    /** 해당 주문 내 딜러상품 수량 */
    private Integer dealerItemCount;

    /** 관리자 메모 */
    private String memo;
}