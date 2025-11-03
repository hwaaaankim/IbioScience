package com.dev.IbioScience.service.product;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.product.select.CategoryDto;
import com.dev.IbioScience.dto.product.select.ProductDetailDto;
import com.dev.IbioScience.dto.product.select.ProductListItemDto;
import com.dev.IbioScience.dto.product.select.ProductReviewDto;
import com.dev.IbioScience.dto.productList.ProductSearchCondition;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.model.product.review.ProductReview;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.product.InternalCategoryLargeRepository;
import com.dev.IbioScience.repository.product.InternalCategoryMediumRepository;
import com.dev.IbioScience.repository.product.InternalCategorySmallRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository;
import com.dev.IbioScience.utils.product.ProductSpecifications;

import lombok.RequiredArgsConstructor;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSelectService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;

    private final CategoryLargeRepository categoryLargeRepository;
    private final CategoryMediumRepository categoryMediumRepository;
    private final CategorySmallRepository categorySmallRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;

    private final InternalCategoryLargeRepository internalCategoryLargeRepository;
    private final InternalCategoryMediumRepository internalCategoryMediumRepository;
    private final InternalCategorySmallRepository internalCategorySmallRepository;

    /** 제품 리스트 조회 (동적 필터 + 페이지네이션) */
    public Page<ProductListItemDto> searchProducts(ProductSearchCondition cond, Pageable pageable) {

        // 중분류/소분류 필터 해석: mediumId -> medium_small_category 통해 연결된 small → small_product_category 통해 productId 집합
        Set<Long> productIdFilter = null;
        if (cond.getMediumId() != null) {
            List<MediumSmallCategory> msc = mediumSmallCategoryRepository.findByMedium_IdOrderBySortOrderAsc(cond.getMediumId());
            Set<Long> smallIds = msc.stream().map(ms -> ms.getSmall().getId()).collect(Collectors.toSet());
            // 소분류-제품 맵핑 엔티티 레포지토리가 현재 제공되지 않았으므로, 여기서는 소분류 ID만 준비.
            // 실제 productId IN 필터는 SmallProductCategory 레포지토리 노출 후 적용 가능.
            // 당장은 medium/large/keyword/brand 등의 정상 필터만 우선 적용.
        }

        Specification<Product> spec = where(ProductSpecifications.hasBrandId(cond.getBrandId()))
                .and(ProductSpecifications.hasDisplayStatus(cond.getDisplayStatus()))
                .and(ProductSpecifications.hasSaleStatus(cond.getSaleStatus()))
                .and(ProductSpecifications.nameOrCodeContains(cond.getKeyword()))
                .and(ProductSpecifications.priceBetween(cond.getMinPrice(), cond.getMaxPrice()))
                .and(ProductSpecifications.validOn(cond.getValidOn()));

        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductListItemDto> content = page.getContent().stream()
                .map(p -> {
                    String mainUrl = p.getImages() == null ? null :
                            p.getImages().stream()
                                    .filter(img -> img.getType() == ProductImageType.MAIN)
                                    .sorted(Comparator.comparing(img -> Optional.ofNullable(img.getSortOrder()).orElse(9999)))
                                    .map(ProductImage::getUrl)
                                    .findFirst().orElse(null);

                    Double avg = productReviewRepository.getAverageRating(p.getId());
                    Long cnt = productReviewRepository.getReviewCount(p.getId());

                    return ProductListItemDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .code(p.getCode())
                            .salePrice(p.getSalePrice())
                            .consumerPrice(p.getConsumerPrice())
                            .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                            .mainImageUrl(mainUrl)
                            .displayStatus(p.getDisplayStatus())
                            .saleStatus(p.getSaleStatus())
                            .newState(p.getNewState())
                            .averageRating(avg == null ? 0.0 : avg)
                            .reviewCount(cnt == null ? 0L : cnt)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /** 제품 상세 조회 (연관 항목 포함) */
    public ProductDetailDto getProductDetail(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + productId));

        Double avg = productReviewRepository.getAverageRating(productId);
        Long cnt = productReviewRepository.getReviewCount(productId);

        // 내부 카테고리 체인
        String icSmall = null, icMedium = null, icLarge = null;
        if (p.getInternalCategorySmall() != null) {
            InternalCategorySmall small = p.getInternalCategorySmall();
            icSmall = small.getName();
            if (small.getMedium() != null) {
                icMedium = small.getMedium().getName();
                if (small.getMedium().getLarge() != null) {
                    icLarge = small.getMedium().getLarge().getName();
                }
            }
        }

        return ProductDetailDto.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .displayStatus(p.getDisplayStatus())
                .saleStatus(p.getSaleStatus())
                .summaryDescription(p.getSummaryDescription())
                .shortDescription(p.getShortDescription())
                .detailHtml(p.getDetailHtml())
                .salePrice(p.getSalePrice())
                .consumerPrice(p.getConsumerPrice())
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .iconUrl(p.getIconUrl())
                .useIconPeriod(p.getUseIconPeriod())
                .iconStartDate(p.getIconStartDate())
                .iconEndDate(p.getIconEndDate())
                .internalCategorySmallName(icSmall)
                .internalCategoryMediumName(icMedium)
                .internalCategoryLargeName(icLarge)
                .images(p.getImages() == null ? List.of() :
                        p.getImages().stream()
                                .sorted(Comparator.comparing(img -> Optional.ofNullable(img.getSortOrder()).orElse(9999)))
                                .map(img -> ProductDetailDto.ImageDto.builder()
                                        .url(img.getUrl())
                                        .path(img.getPath())
                                        .fileName(img.getFileName())
                                        .sortOrder(img.getSortOrder())
                                        .build())
                                .collect(Collectors.toList()))
                .detailImages(p.getDetailImages() == null ? List.of() :
                        p.getDetailImages().stream()
                                .sorted(Comparator.comparing(img -> Optional.ofNullable(img.getSortOrder()).orElse(9999)))
                                .map(img -> ProductDetailDto.ImageDto.builder()
                                        .url(img.getUrl())
                                        .path(img.getPath())
                                        .fileName(img.getFileName())
                                        .sortOrder(img.getSortOrder())
                                        .build())
                                .collect(Collectors.toList()))
                .optionGroups(p.getOptionGroups() == null ? List.of() :
                        p.getOptionGroups().stream()
                                .sorted(Comparator.comparing(g -> Optional.ofNullable(g.getSortOrder()).orElse(9999)))
                                .map(g -> ProductDetailDto.OptionGroupDto.builder()
                                        .name(g.getName())
                                        .sortOrder(g.getSortOrder())
                                        .options(g.getOptions() == null ? List.of() :
                                                g.getOptions().stream()
                                                        .sorted(Comparator.comparing(o -> Optional.ofNullable(o.getSortOrder()).orElse(9999)))
                                                        .map(o -> ProductDetailDto.OptionDto.builder()
                                                                .name(o.getName())
                                                                .value(o.getValue())
                                                                .sign(o.getSign() != null ? o.getSign().name() : null)
                                                                .extraPrice(o.getExtraPrice() != null ? o.getExtraPrice().toPlainString() : null)
                                                                .sortOrder(o.getSortOrder())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                .extraFields(p.getExtraFields() == null ? List.of() :
                        p.getExtraFields().stream()
                                .map(f -> ProductDetailDto.ExtraFieldDto.builder()
                                        .label(f.getLabel())
                                        .value(f.getValue())
                                        .build())
                                .collect(Collectors.toList()))
                .bundleItems(p.getBundleItems() == null ? List.of() :
                        p.getBundleItems().stream()
                                .sorted(Comparator.comparing(b -> Optional.ofNullable(b.getSortOrder()).orElse(9999)))
                                .map(b -> ProductDetailDto.BundleItemDto.builder()
                                        .productId(b.getBundleProduct() != null ? b.getBundleProduct().getId() : null)
                                        .productName(b.getBundleProduct() != null ? b.getBundleProduct().getName() : null)
                                        .sortOrder(b.getSortOrder())
                                        .build())
                                .collect(Collectors.toList()))
                .relatedProducts(p.getRelatedProducts() == null ? List.of() :
                        p.getRelatedProducts().stream()
                                .sorted(Comparator.comparing(r -> Optional.ofNullable(r.getSortOrder()).orElse(9999)))
                                .map(r -> ProductDetailDto.RelatedProductDto.builder()
                                        .productId(r.getRelatedProduct() != null ? r.getRelatedProduct().getId() : null)
                                        .productName(r.getRelatedProduct() != null ? r.getRelatedProduct().getName() : null)
                                        .type(r.getType() != null ? r.getType().name() : null)
                                        .sortOrder(r.getSortOrder())
                                        .build())
                                .collect(Collectors.toList()))
                .keywords( // ProductKeyword 매핑 엔티티가 Product에 컬렉션으로 없으므로, 이미 존재한다면 서비스에 레포 주입해 별도 조회 가능
                        List.of()
                )
                .averageRating(avg == null ? 0.0 : avg)
                .reviewCount(cnt == null ? 0L : cnt)
                .build();
    }

    /** 특정 제품의 리뷰 페이지 조회 */
    public Page<ProductReviewDto> getProductReviews(Long productId, Pageable pageable) {
        Page<ProductReview> page = productReviewRepository.findByProduct_Id(productId, pageable);
        List<ProductReviewDto> content = page.getContent().stream()
                .map(r -> ProductReviewDto.builder()
                        .id(r.getId())
                        .memberId(r.getMemberId())
                        .memberDisplayName(r.getMemberDisplayName())
                        .rating(r.getRating())
                        .content(r.getContent())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .images(r.getImages() == null ? List.of() :
                                r.getImages().stream()
                                        .sorted(Comparator.comparing(i -> Optional.ofNullable(i.getSortOrder()).orElse(9999)))
                                        .map(i -> ProductReviewDto.ReviewImageDto.builder()
                                                .url(i.getUrl())
                                                .path(i.getPath())
                                                .fileName(i.getFileName())
                                                .sortOrder(i.getSortOrder())
                                                .build())
                                        .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /** 카테고리(일반) */
    public List<CategoryDto.Large> listLargeCategories() {
        return categoryLargeRepository.findAll().stream()
                .map(l -> CategoryDto.Large.builder().id(l.getId()).name(l.getName()).build())
                .collect(Collectors.toList());
    }

    public List<CategoryDto.Medium> listMediumsByLarge(Long largeId) {
        return categoryMediumRepository.findByLarge_IdOrderByNameAsc(largeId).stream()
                .map(m -> CategoryDto.Medium.builder().id(m.getId()).name(m.getName()).largeId(m.getLarge().getId()).build())
                .collect(Collectors.toList());
    }

    public List<CategoryDto.Small> listSmallsByMedium(Long mediumId) {
        return mediumSmallCategoryRepository.findByMedium_IdOrderBySortOrderAsc(mediumId).stream()
                .map(ms -> CategoryDto.Small.builder().id(ms.getSmall().getId()).name(ms.getSmall().getName()).build())
                .collect(Collectors.toList());
    }

    /** 내부 카테고리 */
    public List<CategoryDto.InternalLarge> listInternalLarge() {
        return internalCategoryLargeRepository.findAll().stream()
                .map(l -> CategoryDto.InternalLarge.builder().id(l.getId()).name(l.getName()).build())
                .collect(Collectors.toList());
    }

    public List<CategoryDto.InternalMedium> listInternalMediumByLarge(Long largeId) {
        return internalCategoryMediumRepository.findByLarge_IdOrderByNameAsc(largeId).stream()
                .map(m -> CategoryDto.InternalMedium.builder().id(m.getId()).name(m.getName()).largeId(m.getLarge().getId()).build())
                .collect(Collectors.toList());
    }

    public List<CategoryDto.InternalSmall> listInternalSmallByMedium(Long mediumId) {
        return internalCategorySmallRepository.findByMedium_IdOrderByNameAsc(mediumId).stream()
                .map(s -> CategoryDto.InternalSmall.builder().id(s.getId()).name(s.getName()).mediumId(s.getMedium().getId()).build())
                .collect(Collectors.toList());
    }
}