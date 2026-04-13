package com.dev.IbioScience.controller.admin.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.settlement.SettlementExecutePreviewResponse;
import com.dev.IbioScience.dto.settlement.SettlementExecuteResultResponse;
import com.dev.IbioScience.dto.settlement.SettlementExecuteSearchRequest;
import com.dev.IbioScience.service.settlement.SettlementExecuteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/settlement-execute")
public class AdminSettlementExecuteApiController {

    private static final String PAGE_CODE = "SHOP_SETTLEMENT_EXECUTE";

    private final SettlementExecuteService settlementExecuteService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/preview")
    public CommonAPIResponse<SettlementExecutePreviewResponse> preview(@RequestBody SettlementExecuteSearchRequest request) {
        return CommonAPIResponse.ok(settlementExecuteService.preview(request));
    }

    @PreAuthorize("@adminMenuFacade.canCreateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/run")
    public CommonAPIResponse<SettlementExecuteResultResponse> run(
        @RequestBody SettlementExecuteSearchRequest request,
        Authentication authentication
    ) {
        return CommonAPIResponse.ok(settlementExecuteService.execute(request, authentication));
    }
}