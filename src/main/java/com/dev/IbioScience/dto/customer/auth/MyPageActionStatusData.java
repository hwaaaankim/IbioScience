package com.dev.IbioScience.dto.customer.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageActionStatusData {

    /** PERSONAL / COMPANY_NOT_SELLER / COMPANY_SELLER / NONE */
    private String mode;

    /** 탈퇴신청 여부(= status == WITHDRAWN) */
    private boolean withdrawApplied;

    /** 기업전환 신청(PENDING) 존재 여부 */
    private boolean companyConversionPending;

    /** 판매딜러전환 신청(PENDING) 존재 여부 */
    private boolean sellerConversionPending;

    /** 버튼 노출 여부(최종) */
    private boolean showWithdrawButton;
    private boolean showCompanyConvertButton;
    private boolean showSellerApplyButton;
}