package com.dev.IbioScience.service.product.front.dealer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.CategoryPathDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ExtraFieldDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.KeywordDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.OptionDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.OptionGroupDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.PricePreviewExampleDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ProductDetailImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ProductImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.QuestionBlockDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewSummaryDto;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.dealer.DealerMediumSmallProductCategory;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductDetailImage;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;
import com.dev.IbioScience.model.product.util.Keyword;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealerProductDetailServiceImpl implements DealerProductDetailService {

    private final DealerProductRepository dealerProductRepository;

    @Override
    public ProductDetailResponseDto getDealerProductDetail(Long dealerProductId) {

        DealerProduct dealerProduct = dealerProductRepository.findById(dealerProductId)
                .orElseThrow(() -> new ProductNotFoundException(dealerProductId));

        validateVisibility(dealerProduct);

        List<CategoryPathDto> categoryPaths = loadCategoryPaths(dealerProduct);
        List<KeywordDto> keywordDtos = loadKeywords(dealerProduct);
        ReviewSummaryDto reviewSummary = buildEmptyReviewSummary();
        List<ReviewDto> reviewDtos = Collections.emptyList();
        List<QuestionBlockDto> questionBlocks = Collections.emptyList();
        PricePreviewExampleDto pricePreviewExample = buildPricePreviewExample(dealerProduct);

        return buildDealerProductDetailDto(
                dealerProduct,
                categoryPaths,
                keywordDtos,
                reviewSummary,
                reviewDtos,
                questionBlocks,
                pricePreviewExample
        );
    }

    /**
     * 상세페이지 진입 가능 여부 검사
     * - 진열 OFF
     * - 상품상태 NORMAL 아님 (삭제대기/삭제)
     * - 판매상태 OFF
     * 모두 진입 불가로 처리
     */
    private void validateVisibility(DealerProduct dealerProduct) {

        if (dealerProduct.getDisplayStatus() != null
                && "OFF".equals(dealerProduct.getDisplayStatus().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "진열하지 않는 딜러상품입니다.");
        }

        if (dealerProduct.getState() != null
                && !"NORMAL".equals(dealerProduct.getState().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "삭제되었거나 삭제대기중인 딜러상품입니다.");
        }

        if (dealerProduct.getSaleStatus() != null
                && "OFF".equals(dealerProduct.getSaleStatus().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "판매하지 않는 딜러상품입니다.");
        }
    }

    // ===================== 카테고리 경로 =====================

    private List<CategoryPathDto> loadCategoryPaths(DealerProduct dealerProduct) {

        if (dealerProduct.getCategoryMappings() == null || dealerProduct.getCategoryMappings().isEmpty()) {
            return Collections.emptyList();
        }

        List<CategoryPathDto> result = new ArrayList<>();

        for (DealerMediumSmallProductCategory mapping : dealerProduct.getCategoryMappings()) {
            if (mapping == null) {
                continue;
            }

            CategoryMedium medium = mapping.getMedium();
            CategorySmall small = mapping.getSmall();
            CategoryLarge large = (medium != null ? medium.getLarge() : null);

            result.add(CategoryPathDto.builder()
                    .largeId(large != null ? large.getId() : null)
                    .largeName(large != null ? large.getName() : null)
                    .mediumId(medium != null ? medium.getId() : null)
                    .mediumName(medium != null ? medium.getName() : null)
                    .smallId(small != null ? small.getId() : null)
                    .smallName(small != null ? small.getName() : null)
                    .build());
        }

        return result.stream()
                .distinct()
                .sorted(Comparator
                        .comparing(CategoryPathDto::getLargeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(CategoryPathDto::getMediumName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(CategoryPathDto::getSmallName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    // ===================== 키워드 =====================

    private List<KeywordDto> loadKeywords(DealerProduct dealerProduct) {

        if (dealerProduct.getKeywordMappings() == null || dealerProduct.getKeywordMappings().isEmpty()) {
            return Collections.emptyList();
        }

        return dealerProduct.getKeywordMappings().stream()
                .filter(mapping -> mapping != null && mapping.getKeyword() != null)
                .map(mapping -> {
                    Keyword keyword = mapping.getKeyword();
                    return KeywordDto.builder()
                            .id(keyword.getId())
                            .word(keyword.getWord())
                            .build();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    // ===================== 리뷰 =====================

    private ReviewSummaryDto buildEmptyReviewSummary() {
        return ReviewSummaryDto.builder()
                .averageRating(0.0)
                .reviewCount(0L)
                .build();
    }

    // ===================== 가격 예시 =====================

    private PricePreviewExampleDto buildPricePreviewExample(DealerProduct dealerProduct) {

        Integer salePrice = dealerProduct.getSalePrice();
        if (salePrice == null) {
            salePrice = 0;
        }

        boolean visibleForGuest = dealerProduct.getPriceExposeTarget() != null
                && "GUEST".equals(dealerProduct.getPriceExposeTarget().name());

        boolean visibleForMember = true;

        String description =
                "딜러상품 상세는 현재 일반회원 기준 기본 판매가만 예시로 내려주고 있습니다. "
                + "브랜드/공통표시사항/프로모션/등급혜택/번들/연관상품은 딜러상품 상세에서 사용하지 않습니다.";

        return PricePreviewExampleDto.builder()
                .priceVisibleForGuest(visibleForGuest)
                .priceVisibleForMember(visibleForMember)
                .baseSalePrice(salePrice)
                .exampleNormalMemberPrice(salePrice)
                .exampleDealerAPrice(null)
                .useReplacementText(Boolean.TRUE.equals(dealerProduct.getUsePriceReplacementText()))
                .replacementText(dealerProduct.getPriceReplacementText())
                .description(description)
                .build();
    }

    // ===================== 최종 DTO 조립 =====================

    private ProductDetailResponseDto buildDealerProductDetailDto(
            DealerProduct dealerProduct,
            List<CategoryPathDto> categoryPaths,
            List<KeywordDto> keywordDtos,
            ReviewSummaryDto reviewSummary,
            List<ReviewDto> reviewDtos,
            List<QuestionBlockDto> questionBlocks,
            PricePreviewExampleDto pricePreviewExample
    ) {

        List<ProductImageDto> imageDtos = Collections.emptyList();
        if (dealerProduct.getImages() != null && !dealerProduct.getImages().isEmpty()) {
            imageDtos = dealerProduct.getImages().stream()
                    .filter(img -> img != null)
                    .sorted(Comparator.comparing(DealerProductImage::getSortOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(img -> ProductImageDto.builder()
                            .id(img.getId())
                            .type(img.getType() != null ? img.getType().name() : null)
                            .url(img.getUrl())
                            .path(img.getPath())
                            .fileName(img.getFileName())
                            .sortOrder(img.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        List<ProductDetailImageDto> detailImageDtos = Collections.emptyList();
        if (dealerProduct.getDetailImages() != null && !dealerProduct.getDetailImages().isEmpty()) {
            detailImageDtos = dealerProduct.getDetailImages().stream()
                    .filter(img -> img != null)
                    .sorted(Comparator.comparing(DealerProductDetailImage::getSortOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(img -> ProductDetailImageDto.builder()
                            .id(img.getId())
                            .url(img.getUrl())
                            .path(img.getPath())
                            .fileName(img.getFileName())
                            .originalFilename(img.getOriginalFilename())
                            .size(img.getSize())
                            .uploadedAt(img.getUploadedAt())
                            .inUse(img.getInUse())
                            .sortOrder(img.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        List<OptionGroupDto> optionGroupDtos = Collections.emptyList();
        if (dealerProduct.getOptionGroups() != null && !dealerProduct.getOptionGroups().isEmpty()) {
            optionGroupDtos = dealerProduct.getOptionGroups().stream()
                    .filter(group -> group != null)
                    .sorted(Comparator.comparing(DealerProductOptionGroup::getSortOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(group -> {
                        List<OptionDto> optionDtos = Collections.emptyList();

                        if (group.getOptions() != null && !group.getOptions().isEmpty()) {
                            optionDtos = group.getOptions().stream()
                                    .filter(opt -> opt != null)
                                    .sorted(Comparator.comparing(DealerProductOption::getSortOrder,
                                            Comparator.nullsLast(Integer::compareTo)))
                                    .map(opt -> OptionDto.builder()
                                            .id(opt.getId())
                                            .name(opt.getName())
                                            .value(opt.getValue())
                                            .extraPrice(opt.getExtraPrice())
                                            .sign(opt.getSign() != null ? opt.getSign().name() : null)
                                            .signLabel(opt.getSign() != null ? opt.getSign().getLabel() : null)
                                            .sortOrder(opt.getSortOrder())
                                            .build())
                                    .collect(Collectors.toList());
                        }

                        return OptionGroupDto.builder()
                                .id(group.getId())
                                .name(group.getName())
                                .sortOrder(group.getSortOrder())
                                .options(optionDtos)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        List<ExtraFieldDto> extraFieldDtos = Collections.emptyList();
        if (dealerProduct.getExtraFields() != null && !dealerProduct.getExtraFields().isEmpty()) {
            extraFieldDtos = dealerProduct.getExtraFields().stream()
                    .filter(field -> field != null)
                    .map(field -> ExtraFieldDto.builder()
                            .id(field.getId())
                            .label(field.getLabel())
                            .value(field.getValue())
                            .build())
                    .collect(Collectors.toList());
        }

        return ProductDetailResponseDto.builder()
                .id(dealerProduct.getId())
                .name(dealerProduct.getName())
                .code(dealerProduct.getCode())
                .displayStatus(dealerProduct.getDisplayStatus() != null ? dealerProduct.getDisplayStatus().name() : null)
                .displayStatusLabel(dealerProduct.getDisplayStatus() != null ? dealerProduct.getDisplayStatus().getLabel() : null)
                .saleStatus(dealerProduct.getSaleStatus() != null ? dealerProduct.getSaleStatus().name() : null)
                .saleStatusLabel(dealerProduct.getSaleStatus() != null ? dealerProduct.getSaleStatus().getLabel() : null)
                .productState(dealerProduct.getState() != null ? dealerProduct.getState().name() : null)
                .productStateLabel(dealerProduct.getState() != null ? dealerProduct.getState().getLabel() : null)
                .newState(dealerProduct.getNewState() != null ? dealerProduct.getNewState().name() : null)
                .newStateLabel(dealerProduct.getNewState() != null ? dealerProduct.getNewState().getLabel() : null)
                .salesCount(dealerProduct.getSalesCount())
                .viewCount(dealerProduct.getViewCount())
                .createdAt(dealerProduct.getCreatedAt())
                .updatedAt(dealerProduct.getUpdatedAt())
                .manufacturerText(dealerProduct.getManufacturerText())
                .supplierText(dealerProduct.getSupplierText())
                .manufacturedAt(dealerProduct.getManufacturedAt())
                .expiredAt(dealerProduct.getExpiredAt())
                .summaryDescription(dealerProduct.getSummaryDescription())
                .shortDescription(dealerProduct.getShortDescription())
                .detailHtml(dealerProduct.getDetailHtml())
                .consumerPrice(dealerProduct.getConsumerPrice())
                .salePrice(dealerProduct.getSalePrice())
                .priceExposeTarget(dealerProduct.getPriceExposeTarget() != null ? dealerProduct.getPriceExposeTarget().name() : null)
                .usePriceReplacementText(dealerProduct.getUsePriceReplacementText())
                .priceReplacementText(dealerProduct.getPriceReplacementText())
                .iconUrl(dealerProduct.getIconUrl())
                .iconPath(dealerProduct.getIconPath())
                .iconFileName(dealerProduct.getIconFileName())
                .useIconPeriod(dealerProduct.getUseIconPeriod())
                .iconStartDate(dealerProduct.getIconStartDate())
                .iconEndDate(dealerProduct.getIconEndDate())

                // 딜러 상세에서는 미사용
                .brand(null)
                .bundleItems(Collections.emptyList())
                .relatedProducts(Collections.emptyList())
                .promotions(Collections.emptyList())
                .gradeBenefits(Collections.emptyList())
                .questions(questionBlocks)

                .categories(categoryPaths)
                .images(imageDtos)
                .detailImages(detailImageDtos)
                .optionGroups(optionGroupDtos)
                .extraFields(extraFieldDtos)
                .keywords(keywordDtos)
                .reviewSummary(reviewSummary)
                .reviews(reviewDtos)
                .pricePreviewExample(pricePreviewExample)
                .build();
    }
}