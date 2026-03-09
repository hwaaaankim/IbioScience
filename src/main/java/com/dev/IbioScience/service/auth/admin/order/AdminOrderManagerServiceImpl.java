package com.dev.IbioScience.service.auth.admin.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.admin.order.AdminOrderBulkStatusUpdateRequest;
import com.dev.IbioScience.dto.admin.order.AdminOrderDetailStatusUpdateRequest;
import com.dev.IbioScience.dto.admin.order.AdminOrderSearchRequest;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderManagerServiceImpl implements AdminOrderManagerService {

    private final OrderRepository orderRepository;

    @Override
    public Page<Order> getOrderPage(AdminOrderSearchRequest req) {
        req.normalize();

        Sort sort = buildSort(req.getSortBy(), req.getSortDir());
        PageRequest pageRequest = PageRequest.of(req.getPage(), req.getSize(), sort);
        Specification<Order> spec = AdminOrderSpecifications.search(req);

        return orderRepository.findAll(spec, pageRequest);
    }

    @Override
    public Order getOrderDetail(Long orderId) {
        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다. id=" + orderId));
    }

    @Override
    @Transactional
    public void updateOrderStatuses(AdminOrderBulkStatusUpdateRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("변경할 주문 상태가 없습니다.");
        }

        for (AdminOrderBulkStatusUpdateRequest.Item item : req.getItems()) {
            if (item.getOrderId() == null || item.getStatus() == null) {
                throw new IllegalArgumentException("주문 상태 변경 값이 올바르지 않습니다.");
            }

            Order order = orderRepository.findById(item.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다. id=" + item.getOrderId()));

            order.setStatus(item.getStatus());
        }
    }

    @Override
    @Transactional
    public void updateOrderDetailStatus(Long orderId, AdminOrderDetailStatusUpdateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("변경 요청 값이 없습니다.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다. id=" + orderId));

        if (req.getStatus() == null) {
            throw new IllegalArgumentException("주문상태는 필수입니다.");
        }
        if (req.getPaymentMethod() == null) {
            throw new IllegalArgumentException("결제수단은 필수입니다.");
        }
        if (req.getShippingMethod() == null) {
            throw new IllegalArgumentException("배송방법은 필수입니다.");
        }
        if (req.getShippingPayType() == null) {
            throw new IllegalArgumentException("배송비 구분은 필수입니다.");
        }

        order.setStatus(req.getStatus());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setShippingMethod(req.getShippingMethod());
        order.setShippingPayType(req.getShippingPayType());
    }

    @Override
    public Map<String, String> getDealerTypeLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(DealerType.NONE.name(), "개인소비자주문");
        map.put(DealerType.BUYER.name(), "구매딜러주문");
        map.put(DealerType.SELLER.name(), "판매딜러주문");
        return map;
    }

    @Override
    public Map<String, String> getOrderStatusLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(OrderStatus.ORDER_COMPLETED.name(), "주문완료");
        map.put(OrderStatus.PRODUCT_PREPARING.name(), "상품준비중");
        map.put(OrderStatus.CANCEL_FINISHED.name(), "취소완료");
        map.put(OrderStatus.DELIVERING.name(), "배송중");
        map.put(OrderStatus.PAYMENT_ERROR.name(), "결제에러");
        return map;
    }

    @Override
    public Map<String, String> getPaymentMethodLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PaymentMethod.ACCOUNT_TRANSFER.name(), "계좌이체");
        map.put(PaymentMethod.CREDIT_CARD.name(), "신용카드");
        map.put(PaymentMethod.PSYS.name(), "PSYS(연구비카드)");
        return map;
    }

    @Override
    public Map<String, String> getShippingMethodLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ShippingMethod.PARCEL.name(), "택배배송");
        map.put(ShippingMethod.POST.name(), "우편배송");
        map.put(ShippingMethod.QUICK.name(), "퀵/당일");
        return map;
    }

    @Override
    public Map<String, String> getShippingPayTypeLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ShippingPayType.PREPAID.name(), "선불");
        map.put(ShippingPayType.COLLECT.name(), "착불");
        return map;
    }

    @Override
    public List<String> getKeywordTypeOptions() {
        return List.of("MEMBER_NAME", "COMPANY_NAME", "SHOP_NAME", "MOBILE", "EMAIL");
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy) {
            case "memberName" -> Sort.by(direction, "member.name").and(Sort.by(Sort.Direction.DESC, "id"));
            case "mobile" -> Sort.by(direction, "member.mobile").and(Sort.by(Sort.Direction.DESC, "id"));
            case "email" -> Sort.by(direction, "member.email").and(Sort.by(Sort.Direction.DESC, "id"));
            case "status" -> Sort.by(direction, "status").and(Sort.by(Sort.Direction.DESC, "id"));
            default -> Sort.by(direction, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }
}