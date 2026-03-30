package com.dev.IbioScience.controller.seller.page;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminRowDto;
import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminSearchCondition;
import com.dev.IbioScience.dto.seller.product.review.DeleteDealerProductReviewsRequest;
import com.dev.IbioScience.service.product.front.dealer.review.DealerProductReviewAdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class DealerProductReviewAdminController {

    private final DealerProductReviewAdminService dealerProductReviewAdminService;

    @GetMapping("/productReviewManager")
    public String productReviewManager(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        DealerProductReviewAdminSearchCondition condition = new DealerProductReviewAdminSearchCondition();
        condition.setPage(Math.max(page, 0));
        condition.setSize(resolvePageSize(size));
        condition.setFromDate(fromDate);
        condition.setToDate(toDate);
        condition.setSortField(resolveSortField(sortField));
        condition.setSortDir(resolveSortDir(sortDir));

        Page<DealerProductReviewAdminRowDto> reviewPage =
                dealerProductReviewAdminService.getAdminReviewPage(condition);

        if (reviewPage.getTotalPages() > 0 && condition.getPage() > reviewPage.getTotalPages() - 1) {
            condition.setPage(reviewPage.getTotalPages() - 1);
            reviewPage = dealerProductReviewAdminService.getAdminReviewPage(condition);
        }

        int startPage = 0;
        int endPage = 0;

        if (reviewPage.getTotalPages() > 0) {
            startPage = Math.max(0, reviewPage.getNumber() - 2);
            endPage = Math.min(reviewPage.getTotalPages() - 1, startPage + 4);

            if (endPage - startPage < 4) {
                startPage = Math.max(0, endPage - 4);
            }
        }

        model.addAttribute("condition", condition);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "administration/seller/product/productReviewManager";
    }

    @PostMapping("/productReviewManager/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSelectedReviews(
            @RequestBody DeleteDealerProductReviewsRequest request
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            int deletedCount = dealerProductReviewAdminService.deleteSelectedReviews(request.getReviewIds());

            result.put("success", true);
            result.put("message", deletedCount + "건의 리뷰가 삭제되었습니다.");
            result.put("deletedCount", deletedCount);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    private int resolvePageSize(int size) {
        if (size == 30 || size == 50 || size == 100) {
            return size;
        }
        return 10;
    }

    private String resolveSortField(String sortField) {
        if ("reviewerId".equalsIgnoreCase(sortField)) {
            return "reviewerId";
        }
        if ("rating".equalsIgnoreCase(sortField)) {
            return "rating";
        }
        return "createdAt";
    }

    private String resolveSortDir(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
    }
}