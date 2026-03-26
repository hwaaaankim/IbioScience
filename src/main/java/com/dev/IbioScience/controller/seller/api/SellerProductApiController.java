package com.dev.IbioScience.controller.seller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.seller.product.DealerProductCreateRequest;
import com.dev.IbioScience.dto.seller.product.DealerProductCreateResponse;
import com.dev.IbioScience.dto.seller.product.EditorTempImageUploadResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductDetailResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductFormMetaResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductUpdateRequest;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.seller.product.SellerProductCommandService;
import com.dev.IbioScience.service.seller.product.SellerProductFileService;
import com.dev.IbioScience.service.seller.product.SellerProductFormQueryService;
import com.dev.IbioScience.service.seller.product.SellerProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seller/api/products")
@RequiredArgsConstructor
public class SellerProductApiController {

    private final SellerProductFormQueryService sellerProductFormQueryService;
    private final SellerProductCommandService sellerProductCommandService;
    private final SellerProductFileService sellerProductFileService;
    private final SellerProductService sellerProductService;

    @GetMapping("/form-meta")
    public ResponseEntity<SellerProductFormMetaResponse> getFormMeta(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        Long loginMemberId = principal.getMember().getId();
        return ResponseEntity.ok(sellerProductFormQueryService.getFormMeta(loginMemberId));
    }

    @PostMapping(value = "/editor/temp-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EditorTempImageUploadResponse> uploadTempEditorImage(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestPart("upload") MultipartFile upload
    ) {
        Long loginMemberId = principal.getMember().getId();
        return ResponseEntity.ok(sellerProductFileService.storeTempEditorImage(loginMemberId, upload));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DealerProductCreateResponse> createProduct(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestPart("request") DealerProductCreateRequest request,
            @RequestPart("representativeImage") MultipartFile representativeImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "iconImage", required = false) MultipartFile iconImage
    ) {
        Long loginMemberId = principal.getMember().getId();

        DealerProductCreateResponse response = sellerProductCommandService.createProduct(
                loginMemberId,
                request,
                representativeImage,
                additionalImages,
                iconImage
        );

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{dealerProductId}")
    public ResponseEntity<SellerProductDetailResponse> getProductDetail(
            @PathVariable Long dealerProductId,
            @AuthenticationPrincipal PrincipalDetails principal,
            Authentication authentication) {

        Long loginMemberId = (principal != null && principal.getMember() != null)
                ? principal.getMember().getId()
                : null;

        boolean adminReadOnlyViewer = isAdminReadOnlyViewer(authentication);

        SellerProductDetailResponse response =
                sellerProductService.getProductDetailForViewer(dealerProductId, loginMemberId, adminReadOnlyViewer);

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{dealerProductId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long dealerProductId,
            @RequestPart("request") SellerProductUpdateRequest request,
            @RequestPart(value = "representativeImage", required = false) MultipartFile representativeImage,
            @RequestPart(value = "iconImage", required = false) MultipartFile iconImage,
            @RequestPart(value = "newAdditionalImageUids", required = false) List<String> newAdditionalImageUids,
            @RequestPart(value = "newAdditionalImages", required = false) List<MultipartFile> newAdditionalImages,
            Authentication authentication
    ) {
    	
    	if (isAdminReadOnlyViewer(authentication)) {
    	    throw new AccessDeniedException("관리자/마스터/루트 계정은 딜러상품을 수정할 수 없습니다.");
    	}
        Long sellerMemberId = resolveSellerMemberId(authentication);

        sellerProductService.updateProduct(
                sellerMemberId,
                dealerProductId,
                request,
                representativeImage,
                iconImage,
                newAdditionalImageUids,
                newAdditionalImages
        );

        Map<String, Object> body = new HashMap<>();
        body.put("message", "상품이 수정되었습니다.");
        body.put("dealerProductId", dealerProductId);
        return ResponseEntity.ok(body);
    }

    private Long resolveSellerMemberId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principalDetails)) {
            throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
        }

        Member member = principalDetails.getMember();
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("로그인 회원 정보를 확인할 수 없습니다.");
        }

        if (member.getDealerType() != DealerType.SELLER || member.getSellerDealerProfile() == null) {
            throw new IllegalArgumentException("판매딜러 회원만 접근할 수 있습니다.");
        }

        return member.getId();
    }
    
    private boolean isAdminReadOnlyViewer(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        boolean hasSellerPortal = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SELLER_PORTAL".equals(a.getAuthority()));

        boolean hasAdminViewerRole = authentication.getAuthorities().stream()
                .anyMatch(a ->
                        "ROLE_ADMIN".equals(a.getAuthority()) ||
                        "ROLE_MASTER".equals(a.getAuthority()) ||
                        "ROLE_ROOT".equals(a.getAuthority())
                );

        return !hasSellerPortal && hasAdminViewerRole;
    }
}