package com.dev.IbioScience.dto.admin.benefit;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponSourceDetailResponse {

    private Long memberCouponId;
    private String couponName;

    private String issueSourceText;
    private LocalDateTime issueOccurredAt;
    private String issueOrderNo;
    private String issueAdminUsername;

    private String usedOrderNo;
    private LocalDateTime usedOccurredAt;
}