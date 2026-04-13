package com.dev.IbioScience.controller.admin.member.api;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.customer.auth.transfer.AdminPageResponse;
import com.dev.IbioScience.dto.customer.auth.transfer.BulkApproveResultDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferApproveRequest;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferBulkApproveRequest;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferDetailDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferRowDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferSearchRequest;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerDealerApproveRequest;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferDetailDto;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferRowDto;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferSearchRequest;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.admin.common.SellerDealerConversionAdminService;
import com.dev.IbioScience.service.auth.customer.common.CompanyConversionAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/root/clientTransfer")
public class ClientTransferManagerApiController {

    private final CompanyConversionAdminService companyConversionAdminService;
    private final SellerDealerConversionAdminService sellerDealerConversionAdminService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_COMPANY_TRANSFER_MANAGER)")
    @GetMapping("/company-applications")
    public CommonAPIResponse<AdminPageResponse<CompanyTransferRowDto>> listCompanyApplications(
            CompanyTransferSearchRequest req) {
        return CommonAPIResponse.ok(companyConversionAdminService.searchPending(req));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_COMPANY_TRANSFER_MANAGER)")
    @GetMapping("/company-applications/{applicationId}")
    public CommonAPIResponse<CompanyTransferDetailDto> companyApplicationDetail(@PathVariable Long applicationId) {
        return CommonAPIResponse.ok(companyConversionAdminService.getDetail(applicationId));
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_COMPANY_TRANSFER_MANAGER)")
    @PostMapping("/company-applications/{applicationId}/approve")
    public CommonAPIResponse<Void> approveCompanyApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) CompanyTransferApproveRequest req,
            @AuthenticationPrincipal PrincipalDetails pd) {

        Long processorId = (pd != null && pd.getMember() != null) ? pd.getMember().getId() : null;
        companyConversionAdminService.approveAndDelete(applicationId, processorId, req != null ? req.getProcessNote() : null);
        return CommonAPIResponse.ok("승인 완료", null);
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_COMPANY_TRANSFER_MANAGER)")
    @PostMapping("/company-applications/bulk-approve")
    public CommonAPIResponse<BulkApproveResultDto> bulkApproveCompanyApplications(
            @RequestBody CompanyTransferBulkApproveRequest req,
            @AuthenticationPrincipal PrincipalDetails pd) {

        Long processorId = (pd != null && pd.getMember() != null) ? pd.getMember().getId() : null;
        BulkApproveResultDto result = companyConversionAdminService.bulkApproveAndDelete(
                req.getApplicationIds(),
                processorId,
                req.getProcessNote()
        );
        return CommonAPIResponse.ok("일괄승인 처리 결과", result);
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/seller-applications")
    public CommonAPIResponse<AdminPageResponse<SellerTransferRowDto>> listSellerApplications(
            SellerTransferSearchRequest req) {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.searchPendingSeller(req));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/seller-applications/{applicationId}")
    public CommonAPIResponse<SellerTransferDetailDto> sellerApplicationDetail(@PathVariable Long applicationId) {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.getDetail(applicationId));
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @PostMapping("/seller-applications/{applicationId}/approve")
    public CommonAPIResponse<Void> approveSellerApplication(
            @PathVariable Long applicationId,
            @RequestBody SellerDealerApproveRequest req,
            @AuthenticationPrincipal PrincipalDetails pd) {

        Long processorId = (pd != null && pd.getMember() != null) ? pd.getMember().getId() : null;
        sellerDealerConversionAdminService.approveSeller(applicationId, processorId, req);
        return CommonAPIResponse.ok("판매딜러 승인 완료", null);
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/categories/larges")
    public CommonAPIResponse<List<Map<String, Object>>> listLarges() {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.listCategoryLarges());
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/categories/mediums")
    public CommonAPIResponse<List<Map<String, Object>>> listMediums(@RequestParam("largeId") Long largeId) {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.listCategoryMediums(largeId));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/categories/smalls")
    public CommonAPIResponse<List<Map<String, Object>>> listSmalls(@RequestParam("mediumId") Long mediumId) {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.listCategorySmallsByMedium(mediumId));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_CLIENT_SELLER_TRANSFER_MANAGER)")
    @GetMapping("/seller-enums")
    public CommonAPIResponse<Map<String, List<String>>> sellerEnums() {
        return CommonAPIResponse.ok(sellerDealerConversionAdminService.getSellerEnums());
    }
}