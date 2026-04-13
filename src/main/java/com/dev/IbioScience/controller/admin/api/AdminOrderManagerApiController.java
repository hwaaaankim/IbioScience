package com.dev.IbioScience.controller.admin.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.admin.order.AdminOrderBulkStatusUpdateRequest;
import com.dev.IbioScience.dto.admin.order.AdminOrderDetailStatusUpdateRequest;
import com.dev.IbioScience.service.auth.admin.order.AdminOrderManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/root/api/orders")
@RequiredArgsConstructor
public class AdminOrderManagerApiController {

    private static final String PAGE_CODE = "ORDER_MANAGER";

    private final AdminOrderManagerService adminOrderManagerService;

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/status/bulk")
    public ResponseEntity<Map<String, Object>> updateBulkStatus(
            @RequestBody AdminOrderBulkStatusUpdateRequest request
    ) {
        adminOrderManagerService.updateOrderStatuses(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "주문 상태가 저장되었습니다.");

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/{orderId}/status-detail")
    public ResponseEntity<Map<String, Object>> updateDetailStatus(
            @PathVariable Long orderId,
            @RequestBody AdminOrderDetailStatusUpdateRequest request
    ) {
        adminOrderManagerService.updateOrderDetailStatus(orderId, request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "주문 상세 상태가 저장되었습니다.");

        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }
}