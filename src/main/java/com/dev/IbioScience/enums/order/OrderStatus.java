package com.dev.IbioScience.enums.order;

public enum OrderStatus {
    ORDER_COMPLETED,        // 주문완료(필요 시 별도 사용)
    PRODUCT_PREPARING,      // 상품준비중
    CANCEL_FINISHED,		// 취소완료
    DELIVERING,             // 배송중
    PAYMENT_ERROR           // 결제에러
}

