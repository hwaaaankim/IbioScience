package com.dev.IbioScience.controller.admin.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListRowDto;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListSearchRequest;
import com.dev.IbioScience.dto.estimate.admin.AdminPageResponse;
import com.dev.IbioScience.dto.estimate.admin.EstimateMailSendRequest;
import com.dev.IbioScience.service.estimate.AdminEstimateListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/client/{memberId}/estimates")
public class AdminEstimateApiController {

    private static final String PAGE_CODE = "CRM_ESTIMATE_LIST";

    private final AdminEstimateListService adminEstimateListService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminEstimateListRowDto>> getEstimateList(
            @PathVariable Long memberId,
            @ModelAttribute AdminEstimateListSearchRequest request
    ) {
        return ResponseEntity.ok(adminEstimateListService.getEstimateList(memberId, request));
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping("/{estimateId}")
    public ResponseEntity<?> getEstimateDetail(
            @PathVariable Long memberId,
            @PathVariable Long estimateId
    ) {
        try {
            return ResponseEntity.ok(adminEstimateListService.getEstimateDetail(memberId, estimateId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(messageBody(e.getMessage()));
        }
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping(
            value = "/{estimateId}/send-email",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> sendEstimateMail(
            @PathVariable Long memberId,
            @PathVariable Long estimateId,
            @RequestPart("request") EstimateMailSendRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) {
        try {
            adminEstimateListService.sendEstimateMail(memberId, estimateId, request, attachments);
            return ResponseEntity.ok(messageBody("견적서 메일이 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(messageBody(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(messageBody(e.getMessage()));
        }
    }

    private Map<String, Object> messageBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return body;
    }
}