package com.dev.IbioScience.dto.settlement;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementExecuteSearchRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private List<SettlementBasis> bases;
    private List<SettlementCycle> cycles;
    private String keyword;
}