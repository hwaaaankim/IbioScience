package com.dev.IbioScience.service.seller.product;

import java.util.List;

import com.dev.IbioScience.dto.seller.product.SellerProductManagerDeleteResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerFilterMetaResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerPageResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerSearchRequest;

public interface SellerProductManagerService {

    SellerProductManagerFilterMetaResponse getFilterMeta(Long loginMemberId);

    SellerProductManagerPageResponse getProductPage(Long loginMemberId, SellerProductManagerSearchRequest request);

    SellerProductManagerDeleteResponse markWaitingDelete(Long loginMemberId, List<Long> dealerProductIds);
}