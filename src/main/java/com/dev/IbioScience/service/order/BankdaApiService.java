package com.dev.IbioScience.service.order;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.bankda.OrderDTO;
import com.dev.IbioScience.dto.bankda.ResultDTO;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.order.OrderItem;
import com.dev.IbioScience.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankdaApiService {

    private final OrderRepository orderRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * /unCheckedOrderLists 응답용
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnCheckedOrderListsData() {
        List<Order> orders = orderRepository
                .findByPaymentMethodAndPaidAtIsNullOrderByCreatedAtAsc(PaymentMethod.ACCOUNT_TRANSFER);

        List<OrderDTO> dtoList = new ArrayList<>();
        for (Order o : orders) {
            dtoList.add(toOrderDTO(o));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orders", dtoList);
        return data;
    }

    /**
     * /orderDetail 응답용
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetailData(Long orderId) {
        Map<String, Object> data = new HashMap<>();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            data.put("return_code", "415");
            data.put("description", "order_id 오류");
            return data;
        }

        data.put("order", toOrderDTO(order));
        return data;
    }

    /**
     * /paymentChecks 응답용
     */
    @Transactional(readOnly = true)
    public Map<String, Object> paymentChecksData(List<Map<String, Object>> requests) {
        Map<String, Object> data = new HashMap<>();

        List<ResultDTO> resultList = new ArrayList<>();
        if (requests == null) requests = List.of();

        for (Map<String, Object> req : requests) {
            String orderIdStr = String.valueOf(req.get("order_id"));
            Long orderId = parseLongSafely(orderIdStr);

            if (orderId == null || !orderRepository.existsById(orderId)) {
                ResultDTO r = new ResultDTO();
                r.setOrder_id(orderIdStr);
                r.setDescription("order_id 오류");
                resultList.add(r);
            }
        }

        if (!resultList.isEmpty()) {
            data.put("return_code", "415");
            data.put("description", "오류 order_id 체크");
            data.put("orders", resultList);
        } else {
            data.put("return_code", "200");
            data.put("description", "정상");
        }

        return data;
    }

    // =========================
    // Mapper
    // =========================
    private OrderDTO toOrderDTO(Order o) {
        OrderDTO dto = new OrderDTO();

        // ----- 기존 필드 -----
        dto.setOrder_id(o.getId());

        String buyerName = (o.getMember() != null ? o.getMember().getName() : null);
        dto.setBuyer_name(buyerName);
        dto.setBilling_name(buyerName); // 기존 코드와 동일 개념으로 우선 동일 값 사용

        // 뱅크다 발급 코드 필요
        dto.setBank_account_no(null);
        dto.setBank_code_name(null);

        if (o.getCreatedAt() != null) {
            dto.setOrder_date(Date.from(o.getCreatedAt().atZone(KST).toInstant()));
        } else {
            dto.setOrder_date(null);
        }

        dto.setOrder_price_amount(o.getGrandTotal() == null ? null : safeLongToInt(o.getGrandTotal()));

        dto.setBuyer_email(o.getMember() != null ? o.getMember().getEmail() : null);
        dto.setBuyer_cellphone(o.getMember() != null ? o.getMember().getMobile() : null);

        // item: OrderItem 기반 Map 리스트
        dto.setItem(toItemMaps(o.getItems()));

        // product_name: 기존 단일 문자열이어서 "첫번째 아이템명" 또는 "외 N건" 형태로 구성
        dto.setProduct_name(buildProductName(o.getItems()));

        // ----- 우리 프로젝트 추가 필드 -----
        dto.setOrder_no(o.getOrderNo());

        dto.setStatus(o.getStatus());
        dto.setPayment_method(o.getPaymentMethod());
        dto.setPaid_at(o.getPaidAt());

        dto.setShipping_method(o.getShippingMethod());
        dto.setShipping_pay_type(o.getShippingPayType());

        dto.setReceiver_name(o.getReceiverName());
        dto.setHp1(o.getHp1());
        dto.setHp2(o.getHp2());
        dto.setHp3(o.getHp3());

        dto.setTel1(o.getTel1());
        dto.setTel2(o.getTel2());
        dto.setTel3(o.getTel3());

        dto.setPostcode(o.getPostcode());
        dto.setRoad_address(o.getRoadAddress());
        dto.setDetail_address(o.getDetailAddress());
        dto.setShipping_memo(o.getShippingMemo());

        dto.setSum_price(o.getSumPrice());
        dto.setShipping_fee(o.getShippingFee());
        dto.setBase_discount(o.getBaseDiscount());
        dto.setCoupon_discount(o.getCouponDiscount());
        dto.setPoint_used(o.getPointUsed());
        dto.setGrand_total(o.getGrandTotal());
        dto.setExpect_point(o.getExpectPoint());

        dto.setCoupon_code(o.getCouponCode());
        dto.setCoupon_name(o.getCouponName());

        dto.setOrderer_name(o.getOrdererName());
        dto.setOrderer_phone(o.getOrdererPhone());
        dto.setOrder_sms_agree(o.getOrderSmsAgree());

        return dto;
    }

    private List<Map<String, Object>> toItemMaps(List<OrderItem> items) {
        if (items == null) return List.of();

        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("order_item_id", it.getId());

            // product / option 참조는 LAZY라서 여기서는 스냅샷 문자열 중심으로 내려드립니다.
            m.put("product_id", it.getProduct() != null ? it.getProduct().getId() : null);

            m.put("product_name", it.getProductName());
            m.put("product_image_url", it.getProductImageUrl());

            m.put("option_group_name", it.getOptionGroupName());
            m.put("option_name", it.getOptionName());
            m.put("option_code", it.getOptionCode());
            m.put("unit_text", it.getUnitText());

            m.put("unit_price", it.getUnitPrice());
            m.put("quantity", it.getQuantity());
            m.put("line_price", it.getLinePrice());
            m.put("item_earn_point", it.getItemEarnPoint());

            list.add(m);
        }
        return list;
    }

    private String buildProductName(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return null;
        String first = items.get(0).getProductName();
        if (items.size() == 1) return first;
        return first + " 외 " + (items.size() - 1) + "건";
    }

    private Integer safeLongToInt(Long v) {
        if (v == null) return null;
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return v.intValue();
    }

    private Long parseLongSafely(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}