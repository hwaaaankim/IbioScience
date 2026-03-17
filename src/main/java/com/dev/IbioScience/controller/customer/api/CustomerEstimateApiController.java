package com.dev.IbioScience.controller.customer.api;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.estimate.EstimateCreateResponse;
import com.dev.IbioScience.dto.estimate.EstimateDeleteRequest;
import com.dev.IbioScience.dto.estimate.EstimateFormInitResponse;
import com.dev.IbioScience.dto.estimate.EstimateListPageResponse;
import com.dev.IbioScience.dto.estimate.EstimateProductSearchResponse;
import com.dev.IbioScience.service.estimate.CustomerEstimateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customer/api/estimate")
@RequiredArgsConstructor
public class CustomerEstimateApiController {

    private final CustomerEstimateService customerEstimateService;

    @GetMapping("/form/init")
    public EstimateFormInitResponse getFormInit(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "mappingId", required = false) Long mappingId
    ) {
        return customerEstimateService.getFormInitData(productId, mappingId);
    }

    @GetMapping("/products")
    public EstimateProductSearchResponse searchProducts(
            @RequestParam(name = "largeId", required = false) Long largeId,
            @RequestParam(name = "mediumId", required = false) Long mediumId,
            @RequestParam(name = "smallId", required = false) Long smallId,
            @RequestParam(name = "productKeyword", required = false) String productKeyword,
            @RequestParam(name = "brandKeyword", required = false) String brandKeyword
    ) {
        return customerEstimateService.searchProducts(largeId, mediumId, smallId, productKeyword, brandKeyword);
    }

    @GetMapping("/brands/suggest")
    public List<String> searchBrandSuggestions(
            @RequestParam(name = "largeId", required = false) Long largeId,
            @RequestParam(name = "mediumId", required = false) Long mediumId,
            @RequestParam(name = "smallId", required = false) Long smallId,
            @RequestParam(name = "productKeyword", required = false) String productKeyword,
            @RequestParam(name = "brandKeyword", required = false) String brandKeyword
    ) {
        return customerEstimateService.searchBrandSuggestions(
                largeId, mediumId, smallId, productKeyword, brandKeyword
        );
    }

    @GetMapping("/products/suggest")
    public List<String> searchProductSuggestions(
            @RequestParam(name = "largeId", required = false) Long largeId,
            @RequestParam(name = "mediumId", required = false) Long mediumId,
            @RequestParam(name = "smallId", required = false) Long smallId,
            @RequestParam(name = "productKeyword", required = false) String productKeyword,
            @RequestParam(name = "brandKeyword", required = false) String brandKeyword
    ) {
        return customerEstimateService.searchProductSuggestions(
                largeId, mediumId, smallId, productKeyword, brandKeyword
        );
    }

    @PostMapping
    public EstimateCreateResponse createEstimate(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "detailContent", required = false) String detailContent,
            @RequestParam(name = "itemsJson") String itemsJson,
            @RequestPart(name = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        return customerEstimateService.createEstimate(
                loginMemberId,
                title,
                detailContent,
                itemsJson,
                files == null ? Collections.emptyList() : files
        );
    }

    @GetMapping("/list")
    public EstimateListPageResponse getMyEstimateList(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "titleKeyword", required = false) String titleKeyword,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "sortBy", defaultValue = "requestedAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        return customerEstimateService.getMyEstimateList(
                loginMemberId, page, size, titleKeyword, from, to, sortBy, sortDir
        );
    }

    @DeleteMapping
    public Map<String, Object> deleteMyEstimates(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestBody EstimateDeleteRequest request
    ) {
        customerEstimateService.deleteMyEstimates(loginMemberId, request.getEstimateIds());
        return Map.of("result", "ok");
    }
}