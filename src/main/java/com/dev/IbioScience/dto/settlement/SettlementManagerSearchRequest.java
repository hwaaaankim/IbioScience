package com.dev.IbioScience.dto.settlement;

import java.util.List;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.settlement.SettlementPayStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementManagerSearchRequest {
    private Integer page = 0;
    private Integer size = 10;
    private List<SettlementBasis> bases;
    private List<SettlementCycle> cycles;
    private SettlementPayStatus payStatus;
    private String keyword;
}