package com.dev.IbioScience.enums.order;

public enum OrderStatus {
    ORDER_COMPLETED,        // 주문완료(필요 시 별도 사용)
//    PAYMENT_PENDING,        // 결제대기중(1번 단계 저장)
//    PAYMENT_COMPLETED,      // 결제완료
    PRODUCT_PREPARING,      // 상품준비중
//    DELIVERY_PREPARING,     // 배송준비중
//    CANCEL_PENDING,         // 취소대기중
    CANCEL_FINISHED,		// 취소완료
    DELIVERING,             // 배송중
    PAYMENT_ERROR           // 결제에러
}

// 입금 전에는 취소 대기가 필요없이 바로 취소완료 되도록 상태관리
