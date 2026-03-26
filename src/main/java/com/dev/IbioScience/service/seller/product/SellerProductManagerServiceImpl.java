package com.dev.IbioScience.service.seller.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.seller.product.SellerProductManagerCategoryLargeResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerCategoryMediumResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerCategorySmallResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerDeleteResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerFilterMetaResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerItemResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerPageResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductManagerSearchRequest;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.dealer.DealerMediumSmallProductCategory;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;
import com.dev.IbioScience.repository.auth.SellerDealerProfileRepository;
import com.dev.IbioScience.repository.product.dealer.DealerMediumSmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductImageRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductManagerServiceImpl implements SellerProductManagerService {

    private final SellerDealerProfileRepository sellerDealerProfileRepository;
    private final DealerProductRepository dealerProductRepository;
    private final DealerProductImageRepository dealerProductImageRepository;
    private final DealerMediumSmallProductCategoryRepository dealerMediumSmallProductCategoryRepository;

    @Override
    public SellerProductManagerFilterMetaResponse getFilterMeta(Long loginMemberId) {
        Long sellerDealerProfileId = getRequiredSellerDealerProfileId(loginMemberId);

        List<DealerMediumSmallProductCategory> visibleMappings =
                dealerMediumSmallProductCategoryRepository.findVisibleMappingsForSeller(
                        sellerDealerProfileId,
                        ProductState.NORMAL
                );

        Map<Long, SellerProductManagerCategoryLargeResponse> largeMap = new LinkedHashMap<>();
        Map<Long, Map<Long, SellerProductManagerCategoryMediumResponse>> mediumMapByLarge = new LinkedHashMap<>();
        Map<Long, Map<Long, SellerProductManagerCategorySmallResponse>> smallMapByMedium = new LinkedHashMap<>();

        for (DealerMediumSmallProductCategory mapping : visibleMappings) {
            CategoryMedium medium = mapping.getMedium();
            CategoryLarge large = medium.getLarge();
            CategorySmall small = mapping.getSmall();

            SellerProductManagerCategoryLargeResponse largeNode =
                    largeMap.computeIfAbsent(
                            large.getId(),
                            key -> SellerProductManagerCategoryLargeResponse.builder()
                                    .id(large.getId())
                                    .name(large.getName())
                                    .mediums(new ArrayList<>())
                                    .build()
                    );

            Map<Long, SellerProductManagerCategoryMediumResponse> mediumMap =
                    mediumMapByLarge.computeIfAbsent(large.getId(), key -> new LinkedHashMap<>());

            SellerProductManagerCategoryMediumResponse mediumNode =
                    mediumMap.computeIfAbsent(
                            medium.getId(),
                            key -> {
                                SellerProductManagerCategoryMediumResponse created =
                                        SellerProductManagerCategoryMediumResponse.builder()
                                                .id(medium.getId())
                                                .name(medium.getName())
                                                .smalls(new ArrayList<>())
                                                .build();
                                largeNode.getMediums().add(created);
                                return created;
                            }
                    );

            Map<Long, SellerProductManagerCategorySmallResponse> smallMap =
                    smallMapByMedium.computeIfAbsent(medium.getId(), key -> new LinkedHashMap<>());

            smallMap.computeIfAbsent(
                    small.getId(),
                    key -> {
                        SellerProductManagerCategorySmallResponse created =
                                SellerProductManagerCategorySmallResponse.builder()
                                        .id(small.getId())
                                        .name(small.getName())
                                        .build();
                        mediumNode.getSmalls().add(created);
                        return created;
                    }
            );
        }

        List<SellerProductManagerCategoryLargeResponse> largeCategories =
                new ArrayList<>(largeMap.values());

        largeCategories.sort(Comparator.comparing(SellerProductManagerCategoryLargeResponse::getName));

        for (SellerProductManagerCategoryLargeResponse large : largeCategories) {
            large.getMediums().sort(Comparator.comparing(SellerProductManagerCategoryMediumResponse::getName));
            for (SellerProductManagerCategoryMediumResponse medium : large.getMediums()) {
                medium.getSmalls().sort(Comparator.comparing(SellerProductManagerCategorySmallResponse::getName));
            }
        }

        return SellerProductManagerFilterMetaResponse.builder()
                .largeCategories(largeCategories)
                .build();
    }

    @Override
    public SellerProductManagerPageResponse getProductPage(Long loginMemberId, SellerProductManagerSearchRequest request) {
        Long sellerDealerProfileId = getRequiredSellerDealerProfileId(loginMemberId);

        Pageable pageable = PageRequest.of(
                request.getValidatedPage(),
                request.getValidatedSize(),
                Sort.by(Sort.Order.desc("id"))
        );

        Page<DealerProduct> page = dealerProductRepository.searchSellerProductPage(
                sellerDealerProfileId,
                request,
                pageable
        );

        List<Long> dealerProductIds = page.getContent().stream()
                .map(DealerProduct::getId)
                .toList();

        Map<Long, String> mainImageUrlMap = getMainImageUrlMap(dealerProductIds);
        Map<Long, List<String>> categoryPathMap = getCategoryPathMap(dealerProductIds);

        List<SellerProductManagerItemResponse> content = page.getContent().stream()
                .map(product -> SellerProductManagerItemResponse.builder()
                        .dealerProductId(product.getId())
                        .code(product.getCode())
                        .categoryPaths(categoryPathMap.getOrDefault(product.getId(), new ArrayList<>()))
                        .mainImageUrl(mainImageUrlMap.get(product.getId()))
                        .name(product.getName())
                        .consumerPrice(product.getConsumerPrice())
                        .salePrice(product.getSalePrice())
                        .saleStatus(product.getSaleStatus() != null ? product.getSaleStatus().name() : null)
                        .saleStatusLabel(product.getSaleStatus() != null ? product.getSaleStatus().getLabel() : "-")
                        .displayStatus(product.getDisplayStatus() != null ? product.getDisplayStatus().name() : null)
                        .displayStatusLabel(product.getDisplayStatus() != null ? product.getDisplayStatus().getLabel() : "-")
                        .detailUrl("/seller/page/product/" + product.getId())
                        .build())
                .toList();

        return SellerProductManagerPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public SellerProductManagerDeleteResponse markWaitingDelete(Long loginMemberId, List<Long> dealerProductIds) {
        Long sellerDealerProfileId = getRequiredSellerDealerProfileId(loginMemberId);

        if (dealerProductIds == null || dealerProductIds.isEmpty()) {
            return SellerProductManagerDeleteResponse.builder()
                    .deletedCount(0)
                    .message("삭제할 상품이 선택되지 않았습니다.")
                    .build();
        }

        List<Long> distinctIds = dealerProductIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return SellerProductManagerDeleteResponse.builder()
                    .deletedCount(0)
                    .message("삭제할 상품이 선택되지 않았습니다.")
                    .build();
        }

        int updated = dealerProductRepository.markWaitingDelete(
                sellerDealerProfileId,
                distinctIds,
                ProductState.NORMAL,
                ProductState.WAITING_DELETE,
                LocalDateTime.now()
        );

        return SellerProductManagerDeleteResponse.builder()
                .deletedCount(updated)
                .message(updated > 0 ? "선택한 상품이 삭제대기 상태로 변경되었습니다." : "변경된 상품이 없습니다.")
                .build();
    }

    private Long getRequiredSellerDealerProfileId(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        return sellerDealerProfileRepository.findByMemberId(loginMemberId)
                .map(SellerDealerProfile::getId)
                .orElseThrow(() -> new AccessDeniedException("판매딜러 프로필을 찾을 수 없습니다."));
    }

    private Map<Long, String> getMainImageUrlMap(List<Long> dealerProductIds) {
        if (dealerProductIds == null || dealerProductIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<DealerProductImage> images = dealerProductImageRepository.findMainImages(
                dealerProductIds,
                ProductImageType.MAIN
        );

        Map<Long, String> result = new LinkedHashMap<>();

        for (DealerProductImage image : images) {
            Long productId = image.getDealerProduct().getId();
            result.putIfAbsent(productId, image.getUrl());
        }

        return result;
    }

    private Map<Long, List<String>> getCategoryPathMap(List<Long> dealerProductIds) {
        if (dealerProductIds == null || dealerProductIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<DealerMediumSmallProductCategory> mappings =
                dealerMediumSmallProductCategoryRepository.findAllByDealerProductIdsWithCategory(dealerProductIds);

        Map<Long, Set<String>> temp = new LinkedHashMap<>();

        for (DealerMediumSmallProductCategory mapping : mappings) {
            Long productId = mapping.getDealerProduct().getId();
            String path = mapping.getMedium().getLarge().getName()
                    + " > " + mapping.getMedium().getName()
                    + " > " + mapping.getSmall().getName();

            temp.computeIfAbsent(productId, key -> new LinkedHashSet<>()).add(path);
        }

        return temp.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}