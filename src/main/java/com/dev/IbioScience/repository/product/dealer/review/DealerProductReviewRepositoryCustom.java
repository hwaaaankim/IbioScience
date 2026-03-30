package com.dev.IbioScience.repository.product.dealer.review;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminRowDto;

public interface DealerProductReviewRepositoryCustom {

    Page<DealerProductReviewAdminRowDto> searchAdminReviewPage(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable,
            String sortField,
            String sortDir
    );
}