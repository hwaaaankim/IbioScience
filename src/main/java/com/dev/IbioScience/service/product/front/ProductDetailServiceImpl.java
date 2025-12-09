package com.dev.IbioScience.service.product.front;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.AnswerDetailImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.AnswerDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.BrandDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.BundleItemDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.CategoryPathDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ExtraFieldDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.GradeBenefitDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.KeywordDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.OptionDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.OptionGroupDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.PricePreviewExampleDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ProductDetailImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ProductImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.PromotionDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.QuestionBlockDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.QuestionOptionDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.RelatedProductDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewImageDto;
import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewSummaryDto;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;
import com.dev.IbioScience.model.product.ProductAnswerDetailImage;
import com.dev.IbioScience.model.product.ProductDetailImage;
import com.dev.IbioScience.model.product.ProductGradeBenefit;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.ProductKeyword;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.model.product.ProductQuestionOption;
import com.dev.IbioScience.model.product.RelatedProduct;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.model.product.review.ProductReview;
import com.dev.IbioScience.model.product.review.ProductReviewImage;
import com.dev.IbioScience.model.product.util.Keyword;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.register.ProductAnswerRepository;
import com.dev.IbioScience.repository.product.register.ProductKeywordRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository.ProductReviewSummaryProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDetailServiceImpl implements ProductDetailService {

    private final ProductRepository productRepository;
    private final SmallProductCategoryRepository smallProductCategoryRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;
    private final ProductKeywordRepository productKeywordRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductAnswerRepository productAnswerRepository;

    @Override
    public ProductDetailResponseDto getProductDetail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // 진열/판매/상태 검사
        validateVisibility(product);

        // ===== 카테고리 경로 =====
        List<CategoryPathDto> categoryPaths = loadCategoryPaths(productId);

        // ===== 키워드 =====
        List<KeywordDto> keywordDtos = loadKeywords(productId);

        // ===== 리뷰 및 요약 =====
        ReviewSummaryDto reviewSummary = loadReviewSummary(productId);
        List<ReviewDto> reviewDtos = loadRecentReviews(productId);

        // ===== 공통질문/답변 =====
        List<QuestionBlockDto> questionBlocks = loadQuestionBlocks(productId);

        // ===== 가격 예시(인증 없는 상태) =====
        PricePreviewExampleDto pricePreviewExample = buildPricePreviewExample(product);

        // ===== 메인 DTO 구성 =====
        return buildProductDetailDto(
                product,
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
    private void validateVisibility(Product product) {

        if (product.getDisplayStatus() == DisplayStatus.OFF) {
            throw new ProductNotDisplayableException(product.getId(), "진열하지 않는 상품입니다.");
        }

        if (product.getState() != null && product.getState() != ProductState.NORMAL) {
            throw new ProductNotDisplayableException(product.getId(), "삭제되었거나 삭제대기중인 상품입니다.");
        }

        if (product.getSaleStatus() != null && product.getSaleStatus().name().equals("OFF")) {
            throw new ProductNotDisplayableException(product.getId(), "판매하지 않는 상품입니다.");
        }
    }

    // ===================== 카테고리 경로 =====================

    private List<CategoryPathDto> loadCategoryPaths(Long productId) {
        List<SmallProductCategory> spcList = smallProductCategoryRepository.findByProductId(productId);
        if (spcList.isEmpty()) {
            return List.of();
        }

        // 소분류 엔티티 모음
        List<CategorySmall> smalls = spcList.stream()
                .map(SmallProductCategory::getSmall)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (smalls.isEmpty()) {
            return List.of();
        }

        List<MediumSmallCategory> mscList = mediumSmallCategoryRepository.findBySmallIn(smalls);

        List<CategoryPathDto> result = new ArrayList<>();

        for (MediumSmallCategory msc : mscList) {
            CategorySmall small = msc.getSmall();
            CategoryMedium medium = msc.getMedium();
            CategoryLarge large = (medium != null) ? medium.getLarge() : null;

            result.add(CategoryPathDto.builder()
                    .largeId(large != null ? large.getId() : null)
                    .largeName(large != null ? large.getName() : null)
                    .mediumId(medium != null ? medium.getId() : null)
                    .mediumName(medium != null ? medium.getName() : null)
                    .smallId(small != null ? small.getId() : null)
                    .smallName(small != null ? small.getName() : null)
                    .build());
        }

        // 중복 제거 + 정렬
        return result.stream()
                .distinct()
                .sorted(Comparator
                        .comparing(CategoryPathDto::getLargeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(CategoryPathDto::getMediumName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(CategoryPathDto::getSmallName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    // ===================== 키워드 =====================

    private List<KeywordDto> loadKeywords(Long productId) {
        List<ProductKeyword> pks = productKeywordRepository.findByProductId(productId);
        return pks.stream()
                .map(pk -> {
                    Keyword k = pk.getKeyword();
                    return KeywordDto.builder()
                            .id(k != null ? k.getId() : null)
                            .word(k != null ? k.getWord() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===================== 리뷰 요약/최근 리뷰 =====================

    private ReviewSummaryDto loadReviewSummary(Long productId) {
        ProductReviewSummaryProjection p = productReviewRepository.findSummaryByProductId(productId);
        if (p == null) {
            return ReviewSummaryDto.builder()
                    .averageRating(0.0)
                    .reviewCount(0L)
                    .build();
        }
        Double avg = (p.getAverageRating() != null) ? p.getAverageRating() : 0.0;
        Long cnt = (p.getReviewCount() != null) ? p.getReviewCount() : 0L;
        return ReviewSummaryDto.builder()
                .averageRating(avg)
                .reviewCount(cnt)
                .build();
    }

    private List<ReviewDto> loadRecentReviews(Long productId) {
        List<ProductReview> reviews = productReviewRepository
                .findTop5ByProductIdOrderByCreatedAtDesc(productId);

        return reviews.stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());
    }

    private ReviewDto toReviewDto(ProductReview r) {
        List<ReviewImageDto> imgs = r.getImages().stream()
                .sorted(Comparator.comparing(ProductReviewImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(img -> ReviewImageDto.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .path(img.getPath())
                        .fileName(img.getFileName())
                        .originalFilename(img.getOriginalFilename())
                        .size(img.getSize())
                        .sortOrder(img.getSortOrder())
                        .uploadedAt(img.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return ReviewDto.builder()
                .id(r.getId())
                .memberId(r.getMemberId())
                .memberDisplayName(r.getMemberDisplayName())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .images(imgs)
                .build();
    }

    // ===================== 공통 질문/답변 =====================

    private List<QuestionBlockDto> loadQuestionBlocks(Long productId) {

        List<ProductAnswer> answers =
                productAnswerRepository.findAllWithQuestionAndImagesByProductId(productId);

        if (answers.isEmpty()) {
            return List.of();
        }

        // questionId 기준으로 그룹핑
        Map<Long, QuestionBlockDto> map = new LinkedHashMap<>();

        for (ProductAnswer pa : answers) {
            ProductQuestion q = pa.getQuestion();
            if (q == null) {
                continue;
            }
            Long qId = q.getId();
            QuestionBlockDto block = map.get(qId);
            if (block == null) {
                // 질문 공통 정보 + 옵션 리스트 세팅
                List<QuestionOptionDto> optionDtos = q.getOptions().stream()
                        .sorted(Comparator.comparing(ProductQuestionOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .map(opt -> QuestionOptionDto.builder()
                                .id(opt.getId())
                                .value(opt.getValue())
                                .sortOrder(opt.getSortOrder())
                                .build())
                        .collect(Collectors.toList());

                block = QuestionBlockDto.builder()
                        .questionId(q.getId())
                        .label(q.getLabel())
                        .type(q.getType() != null ? q.getType().name() : null)
                        .typeLabel(q.getType() != null ? q.getType().getLabel() : null)
                        .required(q.getRequired())
                        .placeholder(q.getPlaceholder())
                        .sortOrder(q.getSortOrder())
                        .options(optionDtos)
                        .answers(new ArrayList<>())
                        .build();

                map.put(qId, block);
            }

            // 답변 + 상세이미지 리스트
            List<AnswerDetailImageDto> imageDtos = pa.getDetailImages().stream()
                    .sorted(Comparator.comparing(ProductAnswerDetailImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .map(di -> AnswerDetailImageDto.builder()
                            .id(di.getId())
                            .url(di.getUrl())
                            .path(di.getPath())
                            .fileName(di.getFileName())
                            .originalFilename(di.getOriginalFilename())
                            .size(di.getSize())
                            .uploadedAt(di.getUploadedAt())
                            .inUse(di.getInUse())
                            .sortOrder(di.getSortOrder())
                            .build())
                    .collect(Collectors.toList());

            AnswerDto answerDto = AnswerDto.builder()
                    .id(pa.getId())
                    .value(pa.getValue())
                    .fileUrl(pa.getFileUrl())
                    .path(pa.getPath())
                    .fileName(pa.getFileName())
                    .detailImages(imageDtos)
                    .build();

            block.getAnswers().add(answerDto);
        }

        return map.values().stream()
                .sorted(Comparator.comparing(QuestionBlockDto::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    // ===================== 가격 예시 =====================

    private PricePreviewExampleDto buildPricePreviewExample(Product product) {

        Integer salePrice = product.getSalePrice();
        if (salePrice == null) {
            salePrice = 0;
        }

        // 가격 노출 대상
        boolean visibleForGuest = product.getPriceExposeTarget() == PriceExposeTarget.GUEST;
        boolean visibleForMember = true; // 회원은 항상 노출된다고 가정

        // 등급별 혜택에서 A등급 하나 예시로 사용
        Integer dealerAPrice = null;
        Optional<ProductGradeBenefit> gradeA = product.getGradeBenefits().stream()
                .filter(g -> g.getDealerGrade() == DealerGrade.A)
                .findFirst();

        if (gradeA.isPresent() && gradeA.get().getDiscountRate() != null && salePrice > 0) {
            BigDecimal rate = gradeA.get().getDiscountRate(); // 예: 10(%) => 10% 할인
            BigDecimal discount = BigDecimal.valueOf(salePrice)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            dealerAPrice = BigDecimal.valueOf(salePrice)
                    .subtract(discount)
                    .intValue();
        }

        String desc = """
                현재는 로그인 정보(회원/딜러 등급)를 알 수 없으므로,
                예시로 '일반회원 = 기본 판매가', '딜러 A등급 = 등급 할인 적용 가격' 정보를 함께 내려줍니다.
                추후 실제 인증 정보를 연동하면, 이 예시 필드를 기반으로 바로 실제 노출 가격 로직으로 교체하면 됩니다.
                """;

        return PricePreviewExampleDto.builder()
                .priceVisibleForGuest(visibleForGuest)
                .priceVisibleForMember(visibleForMember)
                .baseSalePrice(salePrice)
                .exampleNormalMemberPrice(salePrice)
                .exampleDealerAPrice(dealerAPrice)
                .useReplacementText(Boolean.TRUE.equals(product.getUsePriceReplacementText()))
                .replacementText(product.getPriceReplacementText())
                .description(desc)
                .build();
    }

    // ===================== 최종 DTO 조립 =====================

    private ProductDetailResponseDto buildProductDetailDto(
            Product product,
            List<CategoryPathDto> categoryPaths,
            List<KeywordDto> keywordDtos,
            ReviewSummaryDto reviewSummary,
            List<ReviewDto> reviewDtos,
            List<QuestionBlockDto> questionBlocks,
            PricePreviewExampleDto pricePreviewExample
    ) {

        // ===== 브랜드 =====
        Brand brand = product.getBrand();
        BrandDto brandDto = null;
        if (brand != null) {
            brandDto = BrandDto.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .imageUrl(brand.getImageRoad())
                    .build();
        }

        // ===== 이미지 =====
        List<ProductImageDto> imageDtos = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(img -> ProductImageDto.builder()
                        .id(img.getId())
                        .type(img.getType() != null ? img.getType().name() : null)
                        .url(img.getUrl())
                        .path(img.getPath())
                        .fileName(img.getFileName())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        List<ProductDetailImageDto> detailImageDtos = product.getDetailImages().stream()
                .sorted(Comparator.comparing(ProductDetailImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(di -> ProductDetailImageDto.builder()
                        .id(di.getId())
                        .url(di.getUrl())
                        .path(di.getPath())
                        .fileName(di.getFileName())
                        .originalFilename(di.getOriginalFilename())
                        .size(di.getSize())
                        .uploadedAt(di.getUploadedAt())
                        .inUse(di.getInUse())
                        .sortOrder(di.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // ===== 옵션 그룹 =====
        List<OptionGroupDto> optionGroupDtos = product.getOptionGroups().stream()
                .sorted(Comparator.comparing(ProductOptionGroup::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(group -> {
                    List<OptionDto> options = group.getOptions().stream()
                            .sorted(Comparator.comparing(ProductOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
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

                    return OptionGroupDto.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .sortOrder(group.getSortOrder())
                            .options(options)
                            .build();
                })
                .collect(Collectors.toList());

        // ===== 추가입력 필드 =====
        List<ExtraFieldDto> extraFieldDtos = product.getExtraFields().stream()
                .map(f -> ExtraFieldDto.builder()
                        .id(f.getId())
                        .label(f.getLabel())
                        .value(f.getValue())
                        .build())
                .collect(Collectors.toList());

        // ===== 번들 =====
        List<BundleItemDto> bundleItemDtos = product.getBundleItems().stream()
                .sorted(Comparator.comparing(com.dev.IbioScience.model.product.ProductBundleItem::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(b -> BundleItemDto.builder()
                        .id(b.getId())
                        .bundleProductId(b.getBundleProduct() != null ? b.getBundleProduct().getId() : null)
                        .bundleProductName(b.getBundleProduct() != null ? b.getBundleProduct().getName() : null)
                        .sortOrder(b.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // ===== 연관상품 =====
        List<RelatedProductDto> relatedProductDtos = product.getRelatedProducts().stream()
                .sorted(Comparator.comparing(RelatedProduct::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(rp -> RelatedProductDto.builder()
                        .id(rp.getId())
                        .relatedProductId(rp.getRelatedProduct() != null ? rp.getRelatedProduct().getId() : null)
                        .relatedProductName(rp.getRelatedProduct() != null ? rp.getRelatedProduct().getName() : null)
                        .relatedType(rp.getType() != null ? rp.getType().name() : null)
                        .relatedTypeLabel(rp.getType() != null ? rp.getType().getLabel() : null)
                        .sortOrder(rp.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // ===== 프로모션 / 쿠폰 =====
        List<PromotionDto> promotionDtos = product.getDiscountMappings().stream()
                .map(ProductPromotionMapping::getPromotion)
                .filter(Objects::nonNull)
                .distinct()
                .map(p -> PromotionDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .conditionEnabled(p.getConditionEnabled())
                        .iconUrl(p.getIconUrl())
                        .iconPath(p.getIconPath())
                        .active(p.getActive())
                        .type(p.getType() != null ? p.getType().name() : null)
                        .typeLabel(p.getType() != null ? p.getType().getLabel() : null)
                        .term(p.getTerm() != null ? p.getTerm().name() : null)
                        .termLabel(p.getTerm() != null ? p.getTerm().getLabel() : null)
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .couponName(p.getCouponName())
                        .discountPercent(p.getDiscountPercent())
                        .giftProductId(p.getGiftProduct() != null ? p.getGiftProduct().getId() : null)
                        .giftProductName(p.getGiftProduct() != null ? p.getGiftProduct().getName() : null)
                        .target(p.getTarget() != null ? p.getTarget().name() : null)
                        .targetLabel(p.getTarget() != null ? p.getTarget().getLabel() : null)
                        .couponId(p.getCoupon() != null ? p.getCoupon().getId() : null)
                        .build())
                .collect(Collectors.toList());

        // ===== 등급별 혜택 =====
        Integer salePrice = product.getSalePrice() != null ? product.getSalePrice() : 0;
        List<GradeBenefitDto> gradeBenefitDtos = product.getGradeBenefits().stream()
                .map(g -> {
                    Integer examplePrice = null;
                    if (g.getDiscountRate() != null && salePrice > 0) {
                        BigDecimal discount = BigDecimal.valueOf(salePrice)
                                .multiply(g.getDiscountRate())
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                        examplePrice = BigDecimal.valueOf(salePrice)
                                .subtract(discount)
                                .intValue();
                    }
                    return GradeBenefitDto.builder()
                            .id(g.getId())
                            .dealerGrade(g.getDealerGrade() != null ? g.getDealerGrade().name() : null)
                            .discountRate(g.getDiscountRate())
                            .examplePrice(examplePrice)
                            .build();
                })
                .collect(Collectors.toList());

        // ===== 최종 DTO 구성 =====
        return ProductDetailResponseDto.builder()
            .id(product.getId())
            .name(product.getName())
            .code(product.getCode())
            .displayStatus(product.getDisplayStatus() != null ? product.getDisplayStatus().name() : null)
            .displayStatusLabel(product.getDisplayStatus() != null ? product.getDisplayStatus().getLabel() : null)
            .saleStatus(product.getSaleStatus() != null ? product.getSaleStatus().name() : null)
            .saleStatusLabel(product.getSaleStatus() != null ? product.getSaleStatus().getLabel() : null)
            .productState(product.getState() != null ? product.getState().name() : null)
            .productStateLabel(product.getState() != null ? product.getState().getLabel() : null)
            .newState(product.getNewState() != null ? product.getNewState().name() : null)
            .newStateLabel(product.getNewState() != null ? product.getNewState().getLabel() : null)
            .salesCount(product.getSalesCount())
            .viewCount(product.getViewCount())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .manufacturerText(product.getManufacturerText())
            .supplierText(product.getSupplierText())
            .manufacturedAt(product.getManufacturedAt())
            .expiredAt(product.getExpiredAt())
            .summaryDescription(product.getSummaryDescription())
            .shortDescription(product.getShortDescription())
            .detailHtml(product.getDetailHtml())
            .consumerPrice(product.getConsumerPrice())
            .salePrice(product.getSalePrice())
            .priceExposeTarget(product.getPriceExposeTarget() != null ? product.getPriceExposeTarget().name() : null)
            .usePriceReplacementText(product.getUsePriceReplacementText())
            .priceReplacementText(product.getPriceReplacementText())
            .iconUrl(product.getIconUrl())
            .iconPath(product.getIconPath())
            .iconFileName(product.getIconFileName())
            .useIconPeriod(product.getUseIconPeriod())
            .iconStartDate(product.getIconStartDate())
            .iconEndDate(product.getIconEndDate())
            .brand(brandDto)
            .categories(categoryPaths)
            .images(imageDtos)
            .detailImages(detailImageDtos)
            .optionGroups(optionGroupDtos)
            .extraFields(extraFieldDtos)
            .bundleItems(bundleItemDtos)
            .relatedProducts(relatedProductDtos)
            .promotions(promotionDtos)
            .gradeBenefits(gradeBenefitDtos)
            .keywords(keywordDtos)
            .reviewSummary(reviewSummary)
            .reviews(reviewDtos)
            .questions(questionBlocks)
            .pricePreviewExample(pricePreviewExample)
            .build();
    }
}