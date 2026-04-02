package com.dev.IbioScience.controller.seller.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerOrderDetailResponse;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerPageResponse;
import com.dev.IbioScience.dto.seller.settlement.SellerSettlementManagerSearchRequest;
import com.dev.IbioScience.service.seller.settlement.SellerSettlementManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/api/settlement/manager")
public class SellerSettlementManagerApiController {

    private final SellerSettlementManagerService sellerSettlementManagerService;

    @PostMapping("/list")
    public CommonAPIResponse<SellerSettlementManagerPageResponse> getMySettlementList(
            @RequestBody SellerSettlementManagerSearchRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        return CommonAPIResponse.ok(
            sellerSettlementManagerService.getMySettlementPage(username, request)
        );
    }

    @GetMapping("/{settlementId}/orders")
    public CommonAPIResponse<SellerSettlementManagerOrderDetailResponse> getMySettlementOrders(
            @PathVariable Long settlementId,
            Authentication authentication) {

        String username = authentication.getName();

        return CommonAPIResponse.ok(
            sellerSettlementManagerService.getMySettlementOrderDetail(username, settlementId)
        );
    }
}