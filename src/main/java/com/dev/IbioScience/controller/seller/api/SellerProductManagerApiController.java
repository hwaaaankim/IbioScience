package com.dev.IbioScience.controller.seller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dev.IbioScience.dto.seller.product.SellerProductManagerDeleteRequest;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerDeleteResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerFilterMetaResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerPageResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerSearchRequest;
import com.dev.IbioScience.service.seller.product.SellerProductManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/api/products/manager")
public class SellerProductManagerApiController {

    private final SellerProductManagerService sellerProductManagerService;

    @GetMapping("/filter-meta")
    public ResponseEntity<SellerProductManagerFilterMetaResponse> getFilterMeta(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId
    ) {
        validateLogin(loginMemberId);
        return ResponseEntity.ok(sellerProductManagerService.getFilterMeta(loginMemberId));
    }

    @GetMapping("/list")
    public ResponseEntity<SellerProductManagerPageResponse> getList(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @ModelAttribute SellerProductManagerSearchRequest request
    ) {
        validateLogin(loginMemberId);
        return ResponseEntity.ok(sellerProductManagerService.getProductPage(loginMemberId, request));
    }

    @PostMapping("/delete")
    public ResponseEntity<SellerProductManagerDeleteResponse> deleteProducts(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestBody SellerProductManagerDeleteRequest request
    ) {
        validateLogin(loginMemberId);
        return ResponseEntity.ok(
                sellerProductManagerService.markWaitingDelete(loginMemberId, request.getDealerProductIds())
        );
    }

    private void validateLogin(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}