package com.dev.IbioScience.dto.settlement;

import java.util.List;

import com.dev.IbioScience.enums.settlement.SettlementPayStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementStatusUpdateRequest {

    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private Long settlementId;
        private SettlementPayStatus payStatus;
    }
}