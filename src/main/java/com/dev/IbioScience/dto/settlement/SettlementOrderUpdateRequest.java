package com.dev.IbioScience.dto.settlement;

import java.util.List;

import com.dev.IbioScience.enums.settlement.SettlementOrderInclusionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementOrderUpdateRequest {

    private Long settlementId;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private Long settlementOrderId;
        private SettlementOrderInclusionStatus inclusionStatus;
        private String memo;
    }
}