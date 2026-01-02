package com.dev.IbioScience.dto.front.paymentStart;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerCouponResponse {

	private Long memberCouponId;

	private Long couponId;
	private String couponCode;
	private String couponName;

	private BigDecimal minPurchaseAmount;
	private BigDecimal couponAmount;

	private String startDate;   // yyyy-MM-dd
	private String endDate;     // yyyy-MM-dd
	private String issuedDate;  // yyyy-MM-dd (발급일 필터 기준)
}