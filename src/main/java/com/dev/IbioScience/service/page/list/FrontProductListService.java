package com.dev.IbioScience.service.page.list;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.page.productList.DealerDiscountDto;
import com.dev.IbioScience.dto.page.productList.ProductListItemDto;
import com.dev.IbioScience.dto.page.productList.ProductOptionDto;
import com.dev.IbioScience.dto.page.productList.ProductOptionGroupDto;
import com.dev.IbioScience.dto.page.productList.ProductRatingSummaryDto;
import com.dev.IbioScience.dto.page.productList.PromotionSummaryDto;
import com.dev.IbioScience.enums.page.list.ProductSortOption;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.PromotionTarget;
import com.dev.IbioScience.enums.product.PromotionTerm;
import com.dev.IbioScience.enums.product.PromotionType;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductGradeBenefit;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontProductListService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;

    /**
     * 제품 리스트 검색 (페이지)
     */
    public Page<ProductListItemDto> searchProducts(
            Long largeId,
            Long mediumId,
            Long smallId,
            Long brandId,
            String keyword,
            ProductSortOption sortOption,
            int page,
            int size
    ) {
        Pageable pageableForDbSort = buildPageable(sortOption, page, size);

        Page<Product> productPage;

        // 평점 정렬은 별도 쿼리 사용
        if (sortOption == ProductSortOption.RATING_DESC) {
            productPage = productRepository.searchProductsOrderByRatingDesc(
                    largeId, mediumId, smallId, brandId,
                    keyword,
                    DisplayStatus.ON,
                    SaleStatus.ON,
                    ProductState.NORMAL,
                    PageRequest.of(page, size)   // 별점 정렬은 JPQL order by 사용
            );
        } else if (sortOption == ProductSortOption.RATING_ASC) {
            productPage = productRepository.searchProductsOrderByRatingAsc(
                    largeId, mediumId, smallId, brandId,
                    keyword,
                    DisplayStatus.ON,
                    SaleStatus.ON,
                    ProductState.NORMAL,
                    PageRequest.of(page, size)
            );
        } else {
            productPage = productRepository.searchProducts(
                    largeId, mediumId, smallId, brandId,
                    keyword,
                    DisplayStatus.ON,
                    SaleStatus.ON,
                    ProductState.NORMAL,
                    pageableForDbSort
            );
        }

        List<Product> products = productPage.getContent();

        if (products.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageableForDbSort, productPage.getTotalElements());
        }

        Map<Long, ProductRatingSummaryDto> ratingMap = loadRatingSummary(products);

        List<ProductListItemDto> dtoList = products.stream()
                .map(p -> toDto(p, ratingMap.get(p.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, productPage.getPageable(), productPage.getTotalElements());
    }

    /**
     * CATEGORY BEST (판매수 상위 10개)
     * - 현재 조회 조건(분류/브랜드/키워드)을 그대로 적용
     * - 판매수(salesCount) 기준 내림차순
     */
    public List<ProductListItemDto> findCategoryBestProducts(
            Long largeId,
            Long mediumId,
            Long smallId,
            Long brandId,
            String keyword
    ) {
        Pageable topPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "salesCount"));

        Page<Product> productPage = productRepository.searchProducts(
                largeId,
                mediumId,
                smallId,
                brandId,
                keyword,
                DisplayStatus.ON,
                SaleStatus.ON,
                ProductState.NORMAL,
                topPage
        );

        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProductRatingSummaryDto> ratingMap = loadRatingSummary(products);

        return products.stream()
                .map(p -> toDto(p, ratingMap.get(p.getId())))
                .collect(Collectors.toList());
    }

    // ================== 내부 메서드 ==================

    private Pageable buildPageable(ProductSortOption sortOption, int page, int size) {
        Sort sort;
        switch (sortOption) {
            case NAME_ASC:
                sort = Sort.by("name").ascending();
                break;
            case NAME_DESC:
                sort = Sort.by("name").descending();
                break;
            case PRICE_ASC:
                sort = Sort.by("salePrice").ascending();
                break;
            case PRICE_DESC:
                sort = Sort.by("salePrice").descending();
                break;
            case CREATED_AT_DESC:
            default:
                sort = Sort.by("createdAt").descending();
                break;
        }
        return PageRequest.of(page, size, sort);
    }

    private Map<Long, ProductRatingSummaryDto> loadRatingSummary(List<Product> products) {
        List<Long> ids = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        List<ProductRatingSummaryDto> list = productReviewRepository.findRatingSummaryByProductIds(ids);

        return list.stream()
                .collect(Collectors.toMap(
                        ProductRatingSummaryDto::getProductId,
                        r -> r
                ));
    }

    /**
     * Product → DTO 매핑 (가격/프로모션/옵션까지)
     */
    private ProductListItemDto toDto(Product product, ProductRatingSummaryDto rating) {
        ProductListItemDto dto = new ProductListItemDto();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSummaryDescription(product.getSummaryDescription());
        dto.setShortDescription(product.getShortDescription());
        dto.setConsumerPrice(product.getConsumerPrice());
        dto.setSalePrice(product.getSalePrice());
        dto.setNewState(product.getNewState());
        dto.setSalesCount(product.getSalesCount());

        // 브랜드명
        if (product.getBrand() != null) {
            dto.setBrandName(product.getBrand().getName());
        }

        // 대표 이미지
        dto.setMainImageUrl(findMainImageUrl(product));

        // 가격 노출 정책
        dto.setPriceExposeTarget(product.getPriceExposeTarget());
        dto.setUsePriceReplacementText(product.getUsePriceReplacementText());
        dto.setPriceReplacementText(product.getPriceReplacementText());

        // 평점/리뷰
        if (rating != null) {
            dto.setAverageRating(rating.getAverageRating());
            dto.setReviewCount(rating.getReviewCount());
        } else {
            dto.setAverageRating(null);
            dto.setReviewCount(0L);
        }

        // 딜러별 할인
        dto.setDealerDiscounts(buildDealerDiscounts(product));

        // 프로모션 요약 (일반회원 기준)
        PromotionSummaryDto promotionSummary = buildPromotionSummary(product);
        dto.setPromotionSummary(promotionSummary);

        // ✅ 일반회원 기준 기준가 계산
        Integer basePriceForNormal = calculateBasePriceForNormal(product, promotionSummary);

        // ✅ 화면 노출용 가격/문구 결정
        if (Boolean.TRUE.equals(product.getUsePriceReplacementText())
                && product.getPriceReplacementText() != null
                && !product.getPriceReplacementText().isBlank()) {

            dto.setDisplayPrice(null);
            dto.setDisplayPriceText(product.getPriceReplacementText());
        } else {
            dto.setDisplayPrice(basePriceForNormal);
            if (basePriceForNormal != null) {
                dto.setDisplayPriceText(String.format("%,d원", basePriceForNormal));
            } else {
                dto.setDisplayPriceText("-");
            }
        }

        // ✅ 옵션 그룹 + 옵션 (옵션별 최종 가격 계산까지)
        dto.setOptionGroups(buildOptionGroups(product, basePriceForNormal));

        return dto;
    }

    private String findMainImageUrl(Product product) {
        return product.getImages().stream()
                .filter(img -> img.getType() == ProductImageType.MAIN)
                .sorted(Comparator.comparing(
                        img -> Optional.ofNullable(img.getSortOrder()).orElse(0)
                ))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);
    }

    private List<DealerDiscountDto> buildDealerDiscounts(Product product) {
        if (product.getGradeBenefits() == null) {
            return Collections.emptyList();
        }

        return product.getGradeBenefits().stream()
                .sorted(Comparator.comparing(ProductGradeBenefit::getDealerGrade))
                .map(benefit -> {
                    DealerDiscountDto dto = new DealerDiscountDto();
                    dto.setDealerGrade(benefit.getDealerGrade().name());
                    dto.setDiscountRate(benefit.getDiscountRate());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private PromotionSummaryDto buildPromotionSummary(Product product) {
        PromotionSummaryDto summary = new PromotionSummaryDto();

        if (product.getDiscountMappings() == null || product.getDiscountMappings().isEmpty()) {
            summary.setHasPromotion(false);
            summary.setHasNormalPromotion(false);
            summary.setNormalDiscountPercent(null);
            summary.setDiscountedPriceForNormal(null);
            summary.setHasCouponPromotion(false);
            summary.setHasGiftPromotion(false);
            summary.setHasOnePlusOnePromotion(false);
            summary.setCouponNames(Collections.emptyList());
            return summary;
        }

        boolean hasPromotion = false;
        boolean hasNormalPromotion = false;
        boolean hasCouponPromotion = false;
        boolean hasGiftPromotion = false;
        boolean hasOnePlusOnePromotion = false;

        BigDecimal maxNormalDiscount = null;
        List<String> couponNames = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (ProductPromotionMapping mapping : product.getDiscountMappings()) {
            Promotion promo = mapping.getPromotion();
            if (promo == null) continue;
            if (promo.getActive() != null && !promo.getActive()) continue;

            // 기간 체크
            if (!isPromotionActiveForDate(promo, today)) {
                continue;
            }

            hasPromotion = true;

            // 타입별 플래그
            if (promo.getType() == PromotionType.COUPON) {
                hasCouponPromotion = true;
                if (promo.getCouponName() != null) {
                    couponNames.add(promo.getCouponName());
                }
            } else if (promo.getType() == PromotionType.GIFT) {
                hasGiftPromotion = true;
            } else if (promo.getType() == PromotionType.ONE_PLUS_ONE) {
                hasOnePlusOnePromotion = true;
            }

            // 일반회원 대상 할인율
            if (promo.getTarget() == PromotionTarget.NORMAL) {
                hasNormalPromotion = true;
                if (promo.getDiscountPercent() != null) {
                    if (maxNormalDiscount == null ||
                            promo.getDiscountPercent().compareTo(maxNormalDiscount) > 0) {
                        maxNormalDiscount = promo.getDiscountPercent();
                    }
                }
            }
        }

        summary.setHasPromotion(hasPromotion);
        summary.setHasNormalPromotion(hasNormalPromotion);
        summary.setNormalDiscountPercent(maxNormalDiscount);
        summary.setHasCouponPromotion(hasCouponPromotion);
        summary.setHasGiftPromotion(hasGiftPromotion);
        summary.setHasOnePlusOnePromotion(hasOnePlusOnePromotion);
        summary.setCouponNames(couponNames);

        // 일반회원 기준 최종가 계산 (프로모션 할인만 반영)
        if (hasNormalPromotion && maxNormalDiscount != null
                && product.getSalePrice() != null) {
            BigDecimal base = BigDecimal.valueOf(product.getSalePrice());
            BigDecimal rate = maxNormalDiscount.divide(BigDecimal.valueOf(100));
            BigDecimal discounted = base.subtract(base.multiply(rate));
            summary.setDiscountedPriceForNormal(
                    discounted.setScale(0, RoundingMode.HALF_UP).intValue()
            );
        } else {
            summary.setDiscountedPriceForNormal(product.getSalePrice());
        }

        return summary;
    }

    private boolean isPromotionActiveForDate(Promotion promo, LocalDate today) {
        if (promo.getTerm() == PromotionTerm.ALWAYS) {
            return true;
        }

        LocalDate start = promo.getStartDate();
        LocalDate end = promo.getEndDate();

        if (start != null && today.isBefore(start)) {
            return false;
        }
        if (end != null && today.isAfter(end)) {
            return false;
        }
        return true;
    }

    /**
     * 일반회원 기준 기준가 계산
     */
    private Integer calculateBasePriceForNormal(Product product, PromotionSummaryDto summary) {
        if (summary != null && summary.getDiscountedPriceForNormal() != null) {
            return summary.getDiscountedPriceForNormal();
        }
        if (product.getSalePrice() != null) {
            return product.getSalePrice();
        }
        if (product.getConsumerPrice() != null) {
            return product.getConsumerPrice();
        }
        return null;
    }

    /**
     * 옵션 그룹 + 옵션 DTO 구성 (옵션별 최종 가격 계산 포함)
     */
    private List<ProductOptionGroupDto> buildOptionGroups(Product product, Integer basePriceForNormal) {
        if (product.getOptionGroups() == null) {
            return Collections.emptyList();
        }

        return product.getOptionGroups().stream()
                .sorted(Comparator.comparing(
                        g -> Optional.ofNullable(g.getSortOrder()).orElse(0)
                ))
                .map(group -> {
                    ProductOptionGroupDto dto = new ProductOptionGroupDto();
                    dto.setId(group.getId());
                    dto.setName(group.getName());
                    dto.setSortOrder(group.getSortOrder());

                    List<ProductOptionDto> optionDtos = Optional.ofNullable(group.getOptions())
                            .orElse(Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparing(
                                    o -> Optional.ofNullable(o.getSortOrder()).orElse(0)
                            ))
                            .map(opt -> {
                                ProductOptionDto od = new ProductOptionDto();
                                od.setId(opt.getId());
                                od.setName(opt.getName());
                                od.setValue(opt.getValue());
                                od.setExtraPrice(opt.getExtraPrice());
                                od.setSign(opt.getSign() != null ? opt.getSign().name() : null);
                                od.setSortOrder(opt.getSortOrder());

                                // ✅ 옵션별 최종 가격 계산
                                if (basePriceForNormal != null && opt.getExtraPrice() != null && opt.getSign() != null) {
                                    BigDecimal base = BigDecimal.valueOf(basePriceForNormal);
                                    BigDecimal extra = opt.getExtraPrice();

                                    if (opt.getSign() == PriceSign.PLUS) {
                                        base = base.add(extra);
                                    } else if (opt.getSign() == PriceSign.MINUS) {
                                        base = base.subtract(extra);
                                    }
                                    od.setFinalPrice(base.setScale(0, RoundingMode.HALF_UP).intValue());
                                } else if (basePriceForNormal != null) {
                                    od.setFinalPrice(basePriceForNormal);
                                } else {
                                    od.setFinalPrice(null);
                                }

                                return od;
                            })
                            .collect(Collectors.toList());

                    dto.setOptions(optionDtos);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}