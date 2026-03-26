package com.dev.IbioScience.repository.product.dealer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dev.IbioScience.dto.seller.product.SellerProductManagerSearchRequest;
import com.dev.IbioScience.model.product.dealer.DealerProduct;

public interface DealerProductRepositoryCustom {

    Page<DealerProduct> searchSellerProductPage(
            Long sellerDealerProfileId,
            SellerProductManagerSearchRequest condition,
            Pageable pageable
    );
}