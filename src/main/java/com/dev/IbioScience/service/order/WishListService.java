package com.dev.IbioScience.service.order;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.wishList.WishListTargetRequest;
import com.dev.IbioScience.dto.order.WishListProductViewDto;
import com.dev.IbioScience.dto.order.WishToggleResponse;
import com.dev.IbioScience.enums.order.WishToggleAction;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.enums.product.dealer.WishListProductType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.order.WishListItem;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.order.WishListItemRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductImageRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;
import com.dev.IbioScience.repository.product.register.ProductImageRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishListService {

    private final WishListItemRepository wishListItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final DealerProductRepository dealerProductRepository;

    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final DealerProductOptionGroupRepository dealerProductOptionGroupRepository;

    private final ProductImageRepository productImageRepository;
    private final DealerProductImageRepository dealerProductImageRepository;

    @Transactional(readOnly = true)
    public long countByMemberId(Long memberId) {
        if (memberId == null) {
            return 0L;
        }
        return wishListItemRepository.countByMember_Id(memberId);
    }

    @Transactional
    public void add(Long memberId, WishListProductType productType, Long targetId) {
        validateRequest(memberId, productType, targetId);

        if (exists(memberId, productType, targetId)) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        WishListItem.WishListItemBuilder builder = WishListItem.builder()
                .member(member)
                .wishlistProductType(productType);

        if (productType == WishListProductType.COMPANY) {
            Product product = productRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("회사상품이 존재하지 않습니다. id=" + targetId));
            builder.product(product);
        } else if (productType == WishListProductType.DEALER) {
            DealerProduct dealerProduct = dealerProductRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("딜러상품이 존재하지 않습니다. id=" + targetId));
            builder.dealerProduct(dealerProduct);
        } else {
            throw new IllegalArgumentException("지원하지 않는 상품타입입니다. type=" + productType);
        }

        wishListItemRepository.save(builder.build());
    }

    @Transactional
    public WishToggleResponse addWithResult(Long memberId, WishListProductType productType, Long targetId) {
        validateRequest(memberId, productType, targetId);

        WishToggleAction action;
        if (exists(memberId, productType, targetId)) {
            action = WishToggleAction.EXISTS;
        } else {
            add(memberId, productType, targetId);
            action = WishToggleAction.ADDED;
        }

        return new WishToggleResponse(countByMemberId(memberId), action);
    }

    @Transactional
    public void remove(Long memberId, WishListProductType productType, Long targetId) {
        validateRequest(memberId, productType, targetId);
        deleteOne(memberId, productType, targetId);
    }

    @Transactional
    public WishToggleResponse toggleWithResult(Long memberId, WishListProductType productType, Long targetId) {
        validateRequest(memberId, productType, targetId);

        WishToggleAction action;
        if (exists(memberId, productType, targetId)) {
            deleteOne(memberId, productType, targetId);
            action = WishToggleAction.REMOVED;
        } else {
            add(memberId, productType, targetId);
            action = WishToggleAction.ADDED;
        }

        return new WishToggleResponse(countByMemberId(memberId), action);
    }

    @Transactional
    public long removeBatch(Long memberId, List<WishListTargetRequest> items) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (items == null || items.isEmpty()) {
            return countByMemberId(memberId);
        }

        for (WishListTargetRequest item : items) {
            if (item == null) {
                continue;
            }
            if (item.getProductType() == null || item.getTargetId() == null) {
                continue;
            }
            deleteOne(memberId, item.getProductType(), item.getTargetId());
        }

        return countByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public Page<WishListProductViewDto> getWishListPage(
            Long memberId,
            SaleStatus saleStatusFilterOrNull,
            Pageable pageable
    ) {
        Page<WishListItem> page = wishListItemRepository
                .findPageByMemberIdAndSaleStatus(memberId, saleStatusFilterOrNull, pageable);

        List<WishListItem> items = page.getContent();

        List<Long> companyProductIds = items.stream()
                .filter(item -> resolveProductType(item) == WishListProductType.COMPANY)
                .map(WishListItem::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> dealerProductIds = items.stream()
                .filter(item -> resolveProductType(item) == WishListProductType.DEALER)
                .map(WishListItem::getDealerProduct)
                .filter(Objects::nonNull)
                .map(DealerProduct::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        final Map<Long, List<ProductOptionGroup>> companyOptionGroupMap =
                loadCompanyOptionGroupMap(companyProductIds);

        final Map<Long, List<DealerProductOptionGroup>> dealerOptionGroupMap =
                loadDealerOptionGroupMap(dealerProductIds);

        final Map<Long, String> companyMainImageUrlMap =
                loadCompanyMainImageUrlMap(companyProductIds);

        final Map<Long, String> dealerMainImageUrlMap =
                loadDealerMainImageUrlMap(dealerProductIds);

        List<WishListProductViewDto> content = items.stream()
                .map(item -> toWishListProductViewDto(
                        item,
                        companyOptionGroupMap,
                        dealerOptionGroupMap,
                        companyMainImageUrlMap,
                        dealerMainImageUrlMap
                ))
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private WishListProductType resolveProductType(WishListItem item) {
        if (item.getWishlistProductType() != null) {
            return item.getWishlistProductType();
        }

        if (item.getProduct() != null && item.getDealerProduct() == null) {
            return WishListProductType.COMPANY;
        }

        if (item.getDealerProduct() != null && item.getProduct() == null) {
            return WishListProductType.DEALER;
        }

        throw new IllegalStateException("관심상품 타입을 판별할 수 없습니다. wishlistItemId=" + item.getId());
    }

    private WishListProductViewDto toWishListProductViewDto(
            WishListItem item,
            Map<Long, List<ProductOptionGroup>> companyOptionGroupMap,
            Map<Long, List<DealerProductOptionGroup>> dealerOptionGroupMap,
            Map<Long, String> companyMainImageUrlMap,
            Map<Long, String> dealerMainImageUrlMap
    ) {
        WishListProductType resolvedType = resolveProductType(item);

        if (resolvedType == WishListProductType.COMPANY) {
            Product product = item.getProduct();
            if (product == null) {
                throw new IllegalStateException("회사상품 관심상품인데 product 가 null 입니다. wishlistItemId=" + item.getId());
            }

            return WishListProductViewDto.from(
                    product,
                    companyOptionGroupMap.getOrDefault(product.getId(), List.of()),
                    companyMainImageUrlMap.get(product.getId())
            );
        }

        if (resolvedType == WishListProductType.DEALER) {
            DealerProduct dealerProduct = item.getDealerProduct();
            if (dealerProduct == null) {
                throw new IllegalStateException("딜러상품 관심상품인데 dealerProduct 가 null 입니다. wishlistItemId=" + item.getId());
            }

            return WishListProductViewDto.fromDealerProduct(
                    dealerProduct,
                    dealerOptionGroupMap.getOrDefault(dealerProduct.getId(), List.of()),
                    dealerMainImageUrlMap.get(dealerProduct.getId()),
                    buildDealerDetailUrl(dealerProduct)
            );
        }

        throw new IllegalStateException("지원하지 않는 wishlistProductType 입니다. wishlistItemId=" + item.getId());
    }

    private Map<Long, List<ProductOptionGroup>> loadCompanyOptionGroupMap(List<Long> companyProductIds) {
        if (companyProductIds == null || companyProductIds.isEmpty()) {
            return Map.of();
        }

        List<ProductOptionGroup> groups = productOptionGroupRepository.findWithOptionsByProductIds(companyProductIds);

        return groups.stream()
                .filter(group -> group.getProduct() != null && group.getProduct().getId() != null)
                .collect(Collectors.groupingBy(
                        group -> group.getProduct().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, List<DealerProductOptionGroup>> loadDealerOptionGroupMap(List<Long> dealerProductIds) {
        if (dealerProductIds == null || dealerProductIds.isEmpty()) {
            return Map.of();
        }

        List<DealerProductOptionGroup> groups =
                dealerProductOptionGroupRepository.findWithOptionsByDealerProductIds(dealerProductIds);

        return groups.stream()
                .filter(group -> group.getDealerProduct() != null && group.getDealerProduct().getId() != null)
                .collect(Collectors.groupingBy(
                        group -> group.getDealerProduct().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, String> loadCompanyMainImageUrlMap(List<Long> companyProductIds) {
        if (companyProductIds == null || companyProductIds.isEmpty()) {
            return Map.of();
        }

        List<ProductImage> mainImages = productImageRepository.findMainImagesByProductIds(companyProductIds);

        Map<Long, String> result = new HashMap<>();
        for (ProductImage image : mainImages) {
            if (image.getProduct() == null || image.getProduct().getId() == null) {
                continue;
            }

            Long productId = image.getProduct().getId();
            result.putIfAbsent(productId, image.getUrl());
        }
        return result;
    }

    private Map<Long, String> loadDealerMainImageUrlMap(List<Long> dealerProductIds) {
        if (dealerProductIds == null || dealerProductIds.isEmpty()) {
            return Map.of();
        }

        List<DealerProductImage> mainImages =
                dealerProductImageRepository.findMainImagesByDealerProductIds(dealerProductIds, ProductImageType.MAIN);

        Map<Long, String> result = new HashMap<>();
        for (DealerProductImage image : mainImages) {
            if (image.getDealerProduct() == null || image.getDealerProduct().getId() == null) {
                continue;
            }

            Long dealerProductId = image.getDealerProduct().getId();
            result.putIfAbsent(dealerProductId, image.getUrl());
        }
        return result;
    }

    private boolean exists(Long memberId, WishListProductType productType, Long targetId) {
        if (productType == WishListProductType.COMPANY) {
            return wishListItemRepository.existsByMember_IdAndProduct_Id(memberId, targetId);
        }

        if (productType == WishListProductType.DEALER) {
            return wishListItemRepository.existsByMember_IdAndDealerProduct_Id(memberId, targetId);
        }

        throw new IllegalArgumentException("지원하지 않는 상품타입입니다. type=" + productType);
    }

    private void deleteOne(Long memberId, WishListProductType productType, Long targetId) {
        if (productType == WishListProductType.COMPANY) {
            wishListItemRepository.deleteByMember_IdAndProduct_Id(memberId, targetId);
            return;
        }

        if (productType == WishListProductType.DEALER) {
            wishListItemRepository.deleteByMember_IdAndDealerProduct_Id(memberId, targetId);
            return;
        }

        throw new IllegalArgumentException("지원하지 않는 상품타입입니다. type=" + productType);
    }

    private void validateRequest(Long memberId, WishListProductType productType, Long targetId) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (productType == null) {
            throw new IllegalArgumentException("productType 이 필요합니다.");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("targetId 가 필요합니다.");
        }
    }

    private String buildDealerDetailUrl(DealerProduct dealerProduct) {
        if (dealerProduct == null || dealerProduct.getId() == null) {
            return null;
        }

        // TODO: 실제 프론트 딜러상품 상세 URI 로 교체
        // 예: return "/dealerProductDetail/" + dealerProduct.getId();
        return null;
    }
}