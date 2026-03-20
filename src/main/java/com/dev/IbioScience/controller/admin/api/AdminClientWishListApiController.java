package com.dev.IbioScience.controller.admin.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListDeleteRequest;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListDeleteResponse;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListPageResponse;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListSearchCondition;
import com.dev.IbioScience.service.auth.admin.client.AdminClientWishListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/root/api/clientDetail")
@RequiredArgsConstructor
public class AdminClientWishListApiController {

    private final AdminClientWishListService adminClientWishListService;

    @GetMapping("/{memberId}/wishList")
    public AdminClientWishListPageResponse getWishList(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long largeId,
            @RequestParam(required = false) Long mediumId,
            @RequestParam(required = false) Long smallId,
            @RequestParam(required = false) String productName) {

        try {
            AdminClientWishListSearchCondition condition = AdminClientWishListSearchCondition.builder()
                    .page(page)
                    .size(size)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .largeId(largeId)
                    .mediumId(mediumId)
                    .smallId(smallId)
                    .productName(productName)
                    .build();

            return adminClientWishListService.getWishListPage(memberId, condition);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{memberId}/wishList")
    public AdminClientWishListDeleteResponse deleteWishList(
            @PathVariable Long memberId,
            @RequestBody AdminClientWishListDeleteRequest request) {

        try {
            return adminClientWishListService.deleteWishListItems(memberId, request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}