package com.dev.IbioScience.dto.order;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequestDTO {

    /** paymentStart.js payload.userId */
    private Long userId;

    /** 결제수단: ACCOUNT_TRANSFER / CREDIT_CARD / PSYS */
    private String paymentMethod;

    /** 배송방법: PARCEL / POST / QUICK */
    private String shippingMethod;

    /** 선불/착불: PREPAID / COLLECT */
    private String shippingPayType;

    /** 주문자(표시용) */
    private String ordererName;
    private String ordererPhone;
    private Boolean orderSmsAgree;

    /** 배송지 */
    private String receiverName;
    private String hp1;
    private String hp2;
    private String hp3;
    private String tel1;
    private String tel2;
    private String tel3;
    private String postcode;
    private String roadAddress;
    private String detailAddress;
    private String shippingMemo;

    /** 할인/포인트 */
    private Long pointUse;

    /** 선택 쿠폰: memberCouponId (paymentStart.js에서 st.coupon.memberCouponId) */
    private Long memberCouponId;

    /** 아이템(옵션 단위 라인) */
    private List<OrderCreateItemDTO> items;
}