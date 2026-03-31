package com.dev.IbioScience.enums.product;

import lombok.Getter;

/** 정산 주기 */
@Getter
public enum SettlementCycle {
    DAY_1(1),
    DAY_5(5),
    DAY_10(10),
    DAY_15(15),
    DAY_20(20),
    DAY_25(25),
    MONTH_END(-1);

    private final int day;

    SettlementCycle(int day) {
        this.day = day;
    }
}