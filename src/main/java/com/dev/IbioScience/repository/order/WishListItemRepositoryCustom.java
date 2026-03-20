package com.dev.IbioScience.repository.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListSearchCondition;
import com.dev.IbioScience.dto.admin.wishList.WishListProductCategoryPathRow;

public interface WishListItemRepositoryCustom {

    Page<Long> searchWishListItemIds(Long memberId, AdminClientWishListSearchCondition condition, Pageable pageable);

    List<WishListProductCategoryPathRow> findCategoryPathRowsByProductIds(List<Long> productIds);
}