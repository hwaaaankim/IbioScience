package com.dev.IbioScience.enums.settlement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정산 주문 포함 상태
 * - NORMAL   : 정상거래, 정산 포함 기본값
 * - CANCELED : 취소된 거래, 정산 제외 대상
 * - ABNORMAL : 비정상거래, 관리자 검토 필요
 * - HOLD     : 보류, 즉시 지급하지 않음
 */
@Getter
@RequiredArgsConstructor
public enum SettlementOrderInclusionStatus {

    NORMAL("정상거래", true),
    CANCELED("취소된 거래", false),
    ABNORMAL("비정상거래", false),
    HOLD("보류", false);

    private final String label;
    private final boolean included;

    public boolean isIncluded() {
        return included;
    }
}