package com.dev.IbioScience.dto.order;

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
public class OrderDetailItemViewDTO {

    private Long productId;
    private String productName;
    private String productImageUrl;

    private String optionLabel;
    private String unitText;

    private Long unitPrice;
    private Integer quantity;
    private Long discount;   // 현재 0
    private Long subtotal;   // unitPrice*qty (행 기준)

    /** ✅ 라인 적립금 */
    private Long itemEarnPoint;

    /** ✅ 배송비 컬럼 표시용(예: "(공통)") */
    private String shippingText;
}