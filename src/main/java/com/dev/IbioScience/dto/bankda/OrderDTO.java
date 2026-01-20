package com.dev.IbioScience.dto.bankda;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;

import lombok.Data;

@Data
public class OrderDTO {

    // =========================
    // ✅ 기존 bankda 응답 필드 (유지)
    // =========================
    private Long order_id;
    private String buyer_name;
    private String billing_name;
    private String bank_account_no;     // 계좌번호
    private String bank_code_name;      // 은행코드 : 뱅크다발급
    private java.util.Date order_date;  // API 호환 위해 Date 유지
    private Integer order_price_amount;

    private String buyer_email;
    private String buyer_cellphone;

    /**
     * 서비스에서 OrderItem 기반으로 map 생성하여 넣습니다.
     */
    private List<Map<String, Object>> item;

    private String product_name;

    // =========================
    // ✅ 우리 프로젝트 주문 상세 필드 (추가)
    // =========================
    private String order_no;

    private OrderStatus status;
    private PaymentMethod payment_method;
    private LocalDateTime paid_at;

    private ShippingMethod shipping_method;
    private ShippingPayType shipping_pay_type;

    // 배송지 스냅샷
    private String receiver_name;
    private String hp1;
    private String hp2;
    private String hp3;

    private String tel1;
    private String tel2;
    private String tel3;

    private String postcode;
    private String road_address;
    private String detail_address;
    private String shipping_memo;

    // 금액 스냅샷
    private Long sum_price;
    private Long shipping_fee;
    private Long base_discount;
    private Long coupon_discount;
    private Long point_used;
    private Long grand_total;
    private Long expect_point;

    // 쿠폰
    private String coupon_code;
    private String coupon_name;

    // 주문자 정보(표시용)
    private String orderer_name;
    private String orderer_phone;
    private Boolean order_sms_agree;
}