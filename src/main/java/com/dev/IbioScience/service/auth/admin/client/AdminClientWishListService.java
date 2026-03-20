package com.dev.IbioScience.service.auth.admin.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListDeleteRequest;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListDeleteResponse;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListPageResponse;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListRowDTO;
import com.dev.IbioScience.dto.admin.wishList.AdminClientWishListSearchCondition;
import com.dev.IbioScience.dto.admin.wishList.WishListProductCategoryPathRow;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.order.WishListItem;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.repository.order.WishListItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClientWishListService {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50, 70, 100);

    private final WishListItemRepository wishListItemRepository;

    public AdminClientWishListPageResponse getWishListPage(Long memberId, AdminClientWishListSearchCondition condition) {
        validateSearchCondition(condition);

        int normalizedPage = Math.max(condition.getPage(), 0);
        int normalizedSize = normalizePageSize(condition.getSize());

        condition.setPage(normalizedPage);
        condition.setSize(normalizedSize);

        PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize);

        Page<Long> idPage = wishListItemRepository.searchWishListItemIds(memberId, condition, pageRequest);

        if (idPage.isEmpty()) {
            return AdminClientWishListPageResponse.builder()
                    .content(List.of())
                    .page(normalizedPage)
                    .size(normalizedSize)
                    .totalElements(idPage.getTotalElements())
                    .totalPages(idPage.getTotalPages())
                    .numberOfElements(0)
                    .first(idPage.isFirst())
                    .last(idPage.isLast())
                    .build();
        }

        List<WishListItem> wishListItems = wishListItemRepository.findAllWithProductAndBrandAndImagesByIdIn(idPage.getContent());

        Map<Long, WishListItem> wishListItemMap = wishListItems.stream()
                .collect(Collectors.toMap(
                        WishListItem::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<Long> productIds = wishListItems.stream()
                .map(w -> w.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, String> categoryPathMap = buildCategoryPathMap(
                wishListItemRepository.findCategoryPathRowsByProductIds(productIds)
        );

        List<AdminClientWishListRowDTO> content = new ArrayList<>();

        for (Long wishListItemId : idPage.getContent()) {
            WishListItem wishListItem = wishListItemMap.get(wishListItemId);
            if (wishListItem == null) {
                continue;
            }

            Product product = wishListItem.getProduct();

            content.add(AdminClientWishListRowDTO.builder()
                    .wishListItemId(wishListItem.getId())
                    .productId(product.getId())
                    .productName(defaultText(product.getName()))
                    .categoryPath(defaultText(categoryPathMap.get(product.getId())))
                    .brandName(product.getBrand() != null ? defaultText(product.getBrand().getName()) : "-")
                    .mainImageUrl(extractMainImageUrl(product))
                    .build());
        }

        return AdminClientWishListPageResponse.builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .numberOfElements(idPage.getNumberOfElements())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Transactional
    public AdminClientWishListDeleteResponse deleteWishListItems(Long memberId, AdminClientWishListDeleteRequest request) {
        if (request == null || request.getWishListItemIds() == null || request.getWishListItemIds().isEmpty()) {
            throw new IllegalArgumentException("삭제할 관심상품을 선택해 주세요.");
        }

        List<Long> ids = request.getWishListItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 관심상품을 선택해 주세요.");
        }

        long validCount = wishListItemRepository.countByMemberIdAndIdIn(memberId, ids);
        if (validCount != ids.size()) {
            throw new IllegalArgumentException("선택한 관심상품 중 삭제할 수 없는 항목이 포함되어 있습니다.");
        }

        int deletedCount = wishListItemRepository.deleteByMemberIdAndIdIn(memberId, ids);

        return AdminClientWishListDeleteResponse.builder()
                .deletedCount(deletedCount)
                .build();
    }

    private void validateSearchCondition(AdminClientWishListSearchCondition condition) {
        if (condition.getFromDate() != null && condition.getToDate() != null
                && condition.getFromDate().isAfter(condition.getToDate())) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private int normalizePageSize(int size) {
        if (ALLOWED_PAGE_SIZES.contains(size)) {
            return size;
        }
        return 10;
    }

    private Map<Long, String> buildCategoryPathMap(List<WishListProductCategoryPathRow> rows) {
        Map<Long, LinkedHashSet<String>> grouped = new LinkedHashMap<>();

        for (WishListProductCategoryPathRow row : rows) {
            String path = row.getLargeName() + " > " + row.getMediumName() + " > " + row.getSmallName();
            grouped.computeIfAbsent(row.getProductId(), key -> new LinkedHashSet<>()).add(path);
        }

        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), String.join(" | ", entry.getValue()));
        }
        return result;
    }

    private String extractMainImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return "-";
        }

        return product.getImages().stream()
                .filter(image -> image.getType() == ProductImageType.MAIN)
                .sorted(Comparator.comparing(
                        ProductImage::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(ProductImage::getUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("-");
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}