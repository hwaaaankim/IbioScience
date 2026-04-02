package com.dev.IbioScience.dto.seller.settlement;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerSettlementManagerOrderRowDto {

    private Long orderIdSnapshot;
    private String orderNoSnapshot;
    private String ordererNameSnapshot;
    private LocalDateTime basisDateSnapshot;
    private Long dealerItemAmount;
    private Integer dealerItemCount;
}