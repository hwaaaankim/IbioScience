package com.dev.IbioScience.service.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.order.WishListProductViewDto;
import com.dev.IbioScience.dto.order.WishToggleResponse;
import com.dev.IbioScience.enums.order.WishToggleAction;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.order.WishListItem;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.order.WishListItemRepository;
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
    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public long countByMemberId(Long memberId) {
        return wishListItemRepository.countByMember_Id(memberId);
    }

    @Transactional
    public void add(Long memberId, Long productId) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (productId == null) throw new IllegalArgumentException("productId가 필요합니다.");

        if (wishListItemRepository.existsByMember_IdAndProduct_Id(memberId, productId)) return;

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. id=" + productId));

        WishListItem item = WishListItem.builder()
                .member(member)
                .product(product)
                .build();

        wishListItemRepository.save(item);
    }

    /** ✅ 전역용: "추가만" 수행 + ADDED/EXISTS 반환 */
    @Transactional
    public WishToggleResponse addWithResult(Long memberId, Long productId) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (productId == null) throw new IllegalArgumentException("productId가 필요합니다.");

        boolean exists = wishListItemRepository.existsByMember_IdAndProduct_Id(memberId, productId);

        WishToggleAction action;
        if (exists) {
            action = WishToggleAction.EXISTS; // ✅ 삭제하지 않음
        } else {
            add(memberId, productId);
            action = WishToggleAction.ADDED;
        }

        long count = countByMemberId(memberId);
        return new WishToggleResponse(count, action);
    }

    @Transactional
    public void remove(Long memberId, Long productId) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (productId == null) throw new IllegalArgumentException("productId가 필요합니다.");

        wishListItemRepository.deleteByMember_IdAndProduct_Id(memberId, productId);
    }

    /**
     * ✅ 서버가 "이번 요청이 추가/삭제인지"를 확정해서 응답
     */
    @Transactional
    public WishToggleResponse toggleWithResult(Long memberId, Long productId) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (productId == null) throw new IllegalArgumentException("productId가 필요합니다.");

        boolean exists = wishListItemRepository.existsByMember_IdAndProduct_Id(memberId, productId);

        WishToggleAction action;
        if (exists) {
            remove(memberId, productId);
            action = WishToggleAction.REMOVED;
        } else {
            add(memberId, productId);
            action = WishToggleAction.ADDED;
        }

        long count = countByMemberId(memberId);
        return new WishToggleResponse(count, action);
    }

    @Transactional
    public long removeBatch(Long memberId, List<Long> productIds) {
        if (memberId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (productIds == null || productIds.isEmpty()) return countByMemberId(memberId);

        wishListItemRepository.deleteByMember_IdAndProduct_IdIn(memberId, productIds);
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

        List<Long> productIds = page.getContent().stream()
                .map(it -> it.getProduct().getId())
                .distinct()
                .toList();

        List<ProductOptionGroup> groups = productOptionGroupRepository.findWithOptionsByProductIds(productIds);
        Map<Long, List<ProductOptionGroup>> groupMap = groups.stream()
                .collect(Collectors.groupingBy(g -> g.getProduct().getId()));

        Map<Long, String> mainImageUrlMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductImage> mains = productImageRepository.findMainImagesByProductIds(productIds);
            for (ProductImage pi : mains) {
                Long pid = pi.getProduct().getId();
                mainImageUrlMap.putIfAbsent(pid, pi.getUrl());
            }
        }

        List<WishListProductViewDto> content = page.getContent().stream()
                .map(it -> {
                    Product p = it.getProduct();
                    String mainUrl = mainImageUrlMap.get(p.getId());
                    return WishListProductViewDto.from(
                            p,
                            groupMap.getOrDefault(p.getId(), List.of()),
                            mainUrl
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }
}