package com.dev.IbioScience.dto.admin.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderSearchRequest {

    /** 최초 진입과 실제 검색/페이지이동 구분용 */
    private Boolean searchSubmitted = false;

    /** 페이지 */
    private Integer page = 0;

    /** 페이지 사이즈 */
    private Integer size = 10;

    /** 정렬 필드 */
    private String sortBy = "createdAt";

    /** 정렬 방향 */
    private String sortDir = "desc";

    /** 주문일 from */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    /** 주문일 to */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    /** 주문자 분류 */
    private List<DealerType> dealerTypes = new ArrayList<>();

    /** 주문상태 */
    private List<OrderStatus> orderStatuses = new ArrayList<>();

    /** 결제수단 */
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    /** 배송방법 */
    private List<ShippingMethod> shippingMethods = new ArrayList<>();

    /** 배송비 구분 */
    private List<ShippingPayType> shippingPayTypes = new ArrayList<>();

    /** 검색어 타입 */
    private String keywordType;

    /** 검색어 */
    private String keyword;

    public void normalize() {
        if (page == null || page < 0) {
            page = 0;
        }

        if (size == null || !(size == 10 || size == 30 || size == 50 || size == 100)) {
            size = 10;
        }

        if (!StringUtils.hasText(sortBy)) {
            sortBy = "createdAt";
        }

        if (!List.of("createdAt", "memberName", "mobile", "email", "status").contains(sortBy)) {
            sortBy = "createdAt";
        }

        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            sortDir = "desc";
        } else {
            sortDir = sortDir.toLowerCase();
        }

        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isBlank()) {
                keyword = null;
            }
        }

        if (keywordType != null) {
            keywordType = keywordType.trim();
            if (keywordType.isBlank()) {
                keywordType = null;
            }
        }

        // 최초 진입 시 기본값: 전체 체크
        if (!Boolean.TRUE.equals(searchSubmitted)) {
            dealerTypes = new ArrayList<>(Arrays.asList(DealerType.values()));
            orderStatuses = new ArrayList<>(Arrays.asList(OrderStatus.values()));
            paymentMethods = new ArrayList<>(Arrays.asList(PaymentMethod.values()));
            shippingMethods = new ArrayList<>(Arrays.asList(ShippingMethod.values()));
            shippingPayTypes = new ArrayList<>(Arrays.asList(ShippingPayType.values()));
        } else {
            if (dealerTypes == null) dealerTypes = new ArrayList<>();
            if (orderStatuses == null) orderStatuses = new ArrayList<>();
            if (paymentMethods == null) paymentMethods = new ArrayList<>();
            if (shippingMethods == null) shippingMethods = new ArrayList<>();
            if (shippingPayTypes == null) shippingPayTypes = new ArrayList<>();
        }
    }

    public boolean hasAnyDealerType() {
        return dealerTypes != null && !dealerTypes.isEmpty();
    }

    public boolean hasAnyOrderStatus() {
        return orderStatuses != null && !orderStatuses.isEmpty();
    }

    public boolean hasAnyPaymentMethod() {
        return paymentMethods != null && !paymentMethods.isEmpty();
    }

    public boolean hasAnyShippingMethod() {
        return shippingMethods != null && !shippingMethods.isEmpty();
    }

    public boolean hasAnyShippingPayType() {
        return shippingPayTypes != null && !shippingPayTypes.isEmpty();
    }
}