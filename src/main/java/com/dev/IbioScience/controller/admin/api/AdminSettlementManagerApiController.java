package com.dev.IbioScience.controller.admin.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.settlement.SettlementManagerPageResponse;
import com.dev.IbioScience.dto.settlement.SettlementManagerSearchRequest;
import com.dev.IbioScience.dto.settlement.SettlementOrderModalDto;
import com.dev.IbioScience.dto.settlement.SettlementOrderUpdateRequest;
import com.dev.IbioScience.dto.settlement.SettlementStatusUpdateRequest;
import com.dev.IbioScience.service.settlement.SettlementManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/settlement-manager")
public class AdminSettlementManagerApiController {

    private static final String PAGE_CODE = "SHOP_SETTLEMENT_MANAGER";

    private final SettlementManagerService settlementManagerService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/search")
    public CommonAPIResponse<SettlementManagerPageResponse> search(SettlementManagerSearchRequest request) {
        return CommonAPIResponse.ok(settlementManagerService.search(request));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/{settlementId}/orders")
    public CommonAPIResponse<List<SettlementOrderModalDto>> getOrders(@PathVariable Long settlementId) {
        return CommonAPIResponse.ok(settlementManagerService.getSettlementOrders(settlementId));
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PatchMapping("/{settlementId}/orders")
    public CommonAPIResponse<Void> updateOrders(
        @PathVariable Long settlementId,
        @RequestBody SettlementOrderUpdateRequest request
    ) {
        settlementManagerService.updateSettlementOrders(settlementId, request);
        return CommonAPIResponse.ok("주문 반영 및 정산 재계산 완료", null);
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PatchMapping("/status")
    public CommonAPIResponse<Void> updateStatus(@RequestBody SettlementStatusUpdateRequest request) {
        settlementManagerService.updateStatuses(request);
        return CommonAPIResponse.ok("상태 변경 완료", null);
    }
}