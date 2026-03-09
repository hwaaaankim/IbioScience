package com.dev.IbioScience.service.auth.admin.order;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.dev.IbioScience.dto.admin.order.AdminOrderBulkStatusUpdateRequest;
import com.dev.IbioScience.dto.admin.order.AdminOrderDetailStatusUpdateRequest;
import com.dev.IbioScience.dto.admin.order.AdminOrderSearchRequest;
import com.dev.IbioScience.model.order.Order;

public interface AdminOrderManagerService {

    Page<Order> getOrderPage(AdminOrderSearchRequest req);

    Order getOrderDetail(Long orderId);

    void updateOrderStatuses(AdminOrderBulkStatusUpdateRequest req);

    void updateOrderDetailStatus(Long orderId, AdminOrderDetailStatusUpdateRequest req);

    Map<String, String> getDealerTypeLabelMap();

    Map<String, String> getOrderStatusLabelMap();

    Map<String, String> getPaymentMethodLabelMap();

    Map<String, String> getShippingMethodLabelMap();

    Map<String, String> getShippingPayTypeLabelMap();

    List<String> getKeywordTypeOptions();
}