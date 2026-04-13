package com.dev.IbioScience.controller.admin.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.admin.benefit.AdminBenefitActionResponse;
import com.dev.IbioScience.dto.admin.benefit.AdminPointAdjustRequest;
import com.dev.IbioScience.dto.admin.benefit.BenefitPageResponse;
import com.dev.IbioScience.dto.admin.benefit.CouponGrantRequest;
import com.dev.IbioScience.dto.admin.benefit.CouponRowResponse;
import com.dev.IbioScience.dto.admin.benefit.CouponSourceDetailResponse;
import com.dev.IbioScience.dto.admin.benefit.PointHistoryRowResponse;
import com.dev.IbioScience.dto.admin.benefit.PointSummaryResponse;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.service.auth.crm.benefit.AdminClientBenefitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/root/api/clientDetail/{memberId}/benefit")
@RequiredArgsConstructor
public class AdminClientBenefitApiController {

    private static final String PAGE_CODE = "CRM_BENEFIT";

    private final AdminClientBenefitService adminClientBenefitService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/point-summary")
    public PointSummaryResponse getPointSummary(@PathVariable Long memberId) {
        return adminClientBenefitService.getPointSummary(memberId);
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/point-histories")
    public BenefitPageResponse<PointHistoryRowResponse> getPointHistories(
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page
    ) {
        return adminClientBenefitService.getPointHistories(memberId, fromDate, toDate, page);
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/point/grant")
    public PointSummaryResponse grantPoint(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminPointAdjustRequest request
    ) {
        return adminClientBenefitService.grantPoint(memberId, request);
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/point/deduct")
    public PointSummaryResponse deductPoint(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminPointAdjustRequest request
    ) {
        return adminClientBenefitService.deductPoint(memberId, request);
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/coupons")
    public BenefitPageResponse<CouponRowResponse> getCoupons(
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) List<CouponStatus> statuses,
            @RequestParam(defaultValue = "0") int page
    ) {
        return adminClientBenefitService.getCoupons(memberId, fromDate, toDate, statuses, page);
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/coupons/{memberCouponId}/source")
    public CouponSourceDetailResponse getCouponSourceDetail(
            @PathVariable Long memberId,
            @PathVariable Long memberCouponId
    ) {
        return adminClientBenefitService.getCouponSourceDetail(memberId, memberCouponId);
    }

    @PreAuthorize("@adminMenuFacade.canCreateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/coupons/grant")
    public AdminBenefitActionResponse grantCoupon(
            @PathVariable Long memberId,
            @Valid @RequestBody CouponGrantRequest request
    ) {
        adminClientBenefitService.grantCoupon(memberId, request);
        return AdminBenefitActionResponse.builder()
                .message("쿠폰이 발급되었습니다.")
                .build();
    }

    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode('" + PAGE_CODE + "')")
    @DeleteMapping("/coupons/{memberCouponId}")
    public AdminBenefitActionResponse deleteCoupon(
            @PathVariable Long memberId,
            @PathVariable Long memberCouponId
    ) {
        adminClientBenefitService.deleteCoupon(memberId, memberCouponId);
        return AdminBenefitActionResponse.builder()
                .message("쿠폰이 삭제되었습니다.")
                .build();
    }
}