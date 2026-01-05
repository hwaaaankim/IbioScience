package com.dev.IbioScience.controller.customer.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.order.WishToggleResponse;
import com.dev.IbioScience.service.order.WishListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer/wishlist")
public class CustomerWishListApiController {

    private final WishListService wishListService;

    /** 관심상품 개수 */
    @GetMapping("/count")
    public ResponseEntity<Long> count(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId
    ) {
        if (loginMemberId == null) return ResponseEntity.ok(0L);
        return ResponseEntity.ok(wishListService.countByMemberId(loginMemberId));
    }

    /** 관심상품 추가 */
    @PostMapping("/add")
    public ResponseEntity<Long> add(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("productId") Long productId
    ) {
        if (loginMemberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(0L);

        wishListService.add(loginMemberId, productId);
        return ResponseEntity.ok(wishListService.countByMemberId(loginMemberId));
    }

    /** ✅ 전역용: "추가만" + action 포함 응답 (이미 있으면 EXISTS) */
    @PostMapping("/add-check")
    public ResponseEntity<WishToggleResponse> addCheck(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("productId") Long productId
    ) {
        if (loginMemberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WishToggleResponse(0L, null));
        }
        WishToggleResponse res = wishListService.addWithResult(loginMemberId, productId);
        return ResponseEntity.ok(res);
    }

    /** 관심상품 삭제 */
    @PostMapping("/remove")
    public ResponseEntity<Long> remove(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("productId") Long productId
    ) {
        if (loginMemberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(0L);

        wishListService.remove(loginMemberId, productId);
        return ResponseEntity.ok(wishListService.countByMemberId(loginMemberId));
    }

    /** ✅ 토글(있으면 삭제, 없으면 추가) + action 포함 응답 */
    @PostMapping("/toggle")
    public ResponseEntity<WishToggleResponse> toggle(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("productId") Long productId
    ) {
        if (loginMemberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WishToggleResponse(0L, null));
        }

        WishToggleResponse res = wishListService.toggleWithResult(loginMemberId, productId);
        return ResponseEntity.ok(res);
    }

    /** ✅ 선택삭제(배치) */
    @PostMapping("/remove-batch")
    public ResponseEntity<Long> removeBatch(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("productIds") List<Long> productIds
    ) {
        if (loginMemberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(0L);

        long count = wishListService.removeBatch(loginMemberId, productIds);
        return ResponseEntity.ok(count);
    }
}