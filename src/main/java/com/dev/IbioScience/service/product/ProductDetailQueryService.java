package com.dev.IbioScience.service.product;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.ProductQuestionApiDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.BrandReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.BundleProductReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.CategoryPathReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.ExtraFieldReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.IconReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.ImagesReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.OptionGroupReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.OptionReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.PricePolicyReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.ProductAnswerReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.PromotionReadDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO.RelatedProductReadDTO;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.product.InternalCategoryLarge;
import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;
import com.dev.IbioScience.model.product.ProductExtraField;
import com.dev.IbioScience.model.product.ProductGradeBenefit;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductQuestionOption;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.ProductPromotionMappingRepository;
import com.dev.IbioScience.repository.product.ProductQuestionRepository;
import com.dev.IbioScience.repository.product.register.ProductAnswerRepository;
import com.dev.IbioScience.repository.product.register.ProductBundleItemRepository;
import com.dev.IbioScience.repository.product.register.ProductExtraFieldRepository;
import com.dev.IbioScience.repository.product.register.ProductGradeBenefitRepository;
import com.dev.IbioScience.repository.product.register.ProductImageRepository;
import com.dev.IbioScience.repository.product.register.ProductKeywordRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.register.RelatedProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductDetailQueryService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final ProductExtraFieldRepository productExtraFieldRepository; // ✅ 실제 사용
    private final ProductBundleItemRepository productBundleItemRepository;
    private final RelatedProductRepository relatedProductRepository;
    private final ProductPromotionMappingRepository productPromotionMappingRepository;
    private final ProductGradeBenefitRepository productGradeBenefitRepository;
    private final ProductKeywordRepository productKeywordRepository;
    private final SmallProductCategoryRepository smallProductCategoryRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;
    private final ProductAnswerRepository productAnswerRepository;
    private final ProductQuestionRepository productQuestionRepository;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public ProductDetailReadResponseDTO getDetail(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("제품이 존재하지 않습니다. id=" + productId));

        ProductDetailReadResponseDTO dto = new ProductDetailReadResponseDTO();
        dto.setId(p.getId());
        dto.setDisplayStatus(p.getDisplayStatus() != null ? p.getDisplayStatus().name() : null);
        dto.setSaleStatus(p.getSaleStatus() != null ? p.getSaleStatus().name() : null);
        dto.setName(p.getName());
        dto.setCode(p.getCode());

        dto.setSummaryDescription(p.getSummaryDescription());
        dto.setShortDescription(p.getShortDescription());
        dto.setConsumerPrice(p.getConsumerPrice());
        dto.setSalePrice(p.getSalePrice());
        dto.setRewardRate(p.getRewardRate());
        dto.setValidFrom(p.getValidFrom() != null ? p.getValidFrom().format(D) : null);
        dto.setValidTo(p.getValidTo() != null ? p.getValidTo().format(D) : null);
        dto.setNewState(p.getNewState() != null ? p.getNewState().name() : null);

        dto.setManufacturerText(p.getManufacturerText());
        dto.setSupplierText(p.getSupplierText());
        dto.setInternalProductCode(p.getInternalProductCode());
        dto.setManufacturedAt(p.getManufacturedAt() != null ? p.getManufacturedAt().format(D) : null);
        dto.setExpiredAt(p.getExpiredAt() != null ? p.getExpiredAt().format(D) : null);

        // 가격정책
        PricePolicyReadDTO pricePolicy = new PricePolicyReadDTO();
        pricePolicy.setPriceExposeTarget(p.getPriceExposeTarget() != null ? p.getPriceExposeTarget().name() : null);
        pricePolicy.setUsePriceReplacementText(Boolean.TRUE.equals(p.getUsePriceReplacementText()));
        pricePolicy.setPriceReplacementText(p.getPriceReplacementText());
        dto.setPricePolicy(pricePolicy);

        // ✅ 내부 자체분류 (대/중/소 모두)
        if (p.getInternalCategorySmall() != null) {
            InternalCategorySmall s = p.getInternalCategorySmall();
            dto.setInternalCategorySmallId(s.getId());

            InternalCategoryMedium m = s.getMedium(); // 엔티티에 getMedium() 존재 가정(프로젝트 설계상 1:1)
            if (m != null) {
                dto.setInternalCategoryMediumId(m.getId());

                InternalCategoryLarge l = m.getLarge(); // 엔티티에 getLarge() 존재 가정
                if (l != null) {
                    dto.setInternalCategoryLargeId(l.getId());
                }
            }
        } else {
            dto.setInternalCategorySmallId(null);
            dto.setInternalCategoryMediumId(null);
            dto.setInternalCategoryLargeId(null);
        }

        // 브랜드
        if (p.getBrand() != null) {
            BrandReadDTO b = new BrandReadDTO();
            b.setId(p.getBrand().getId());
            b.setName(p.getBrand().getName());
            b.setImageUrl(p.getBrand().getImageRoad()); // 엔티티 필드명 imageRoad 사용
            dto.setBrand(b);
        }

        // 키워드
        dto.setKeywords(
                productKeywordRepository.findByProductWithKeyword(productId).stream()
                        .map(pk -> pk.getKeyword().getWord())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        // 공통질문 정의
        dto.setDisplayQuestions(
                productQuestionRepository.findAllWithOptionsOrder().stream()
                        .map(q -> {
                            // 옵션 정렬 보장
                            List<ProductQuestionOption> sortedOpts =
                                    q.getOptions() == null ? Collections.emptyList()
                                            : q.getOptions().stream()
                                              .sorted(
                                                  Comparator.comparing(
                                                      ProductQuestionOption::getSortOrder,
                                                      Comparator.nullsLast(Integer::compareTo)
                                                  ).thenComparing(ProductQuestionOption::getId)
                                              )
                                              .collect(Collectors.toList());

                            return ProductQuestionApiDTO.from(q, sortedOpts);
                        })
                        .collect(Collectors.toList())
        );

        // 질문 답변
        dto.setAnswers(
                productAnswerRepository.findByProductWithQuestion(productId).stream()
                        .collect(Collectors.groupingBy(a -> a.getQuestion().getId(), LinkedHashMap::new, Collectors.toList()))
                        .entrySet().stream()
                        .map(e -> {
                            ProductAnswer any = e.getValue().get(0);
                            ProductAnswerReadDTO ad = new ProductAnswerReadDTO();
                            ad.setQuestionId(e.getKey());
                            ad.setType(any.getQuestion().getType() != null ? any.getQuestion().getType().name() : null);
                            ad.setValue(any.getValue()); // TEXT/HTML

                            // 파일형일 경우 기존 파일 URL들(답변 상세이미지 포함)
                            List<String> fileUrls = new ArrayList<>();
                            for (ProductAnswer a : e.getValue()) {
                                if (a.getFileUrl() != null) fileUrls.add(a.getFileUrl());
                                if (a.getDetailImages() != null) {
                                    a.getDetailImages().forEach(di -> {
                                        if (di.getUrl() != null) fileUrls.add(di.getUrl());
                                    });
                                }
                            }
                            ad.setFileUrls(fileUrls.isEmpty() ? null : fileUrls);
                            return ad;
                        })
                        .collect(Collectors.toList())
        );

        // 상세설명
        dto.setDetailHtml(p.getDetailHtml());

        // 이미지(대표/추가)
        List<ProductImage> images = productImageRepository.findAllByProductOrder(productId);
        ImagesReadDTO imgs = new ImagesReadDTO();
        imgs.setMainImageUrl(
                images.stream()
                        .filter(i -> i.getType() == ProductImageType.MAIN)
                        .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(ProductImage::getId))
                        .map(ProductImage::getUrl).findFirst().orElse(null)
        );
        imgs.setSubImageUrls(
                images.stream()
                        .filter(i -> i.getType() == ProductImageType.ADDITIONAL)
                        .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(ProductImage::getId))
                        .map(ProductImage::getUrl)
                        .collect(Collectors.toList())
        );
        dto.setImages(imgs);

        // 아이콘
        IconReadDTO icon = new IconReadDTO();
        icon.setImageUrl(p.getIconUrl());
        icon.setUsePeriod(Boolean.TRUE.equals(p.getUseIconPeriod()));
        icon.setStartDate(p.getIconStartDate() != null ? p.getIconStartDate().format(D) : null);
        icon.setEndDate(p.getIconEndDate() != null ? p.getIconEndDate().format(D) : null);
        dto.setIcon(icon);

        // 옵션
        dto.setOptionGroups(
                productOptionGroupRepository.findWithOptions(productId).stream()
                        .map(g -> {
                            OptionGroupReadDTO gd = new OptionGroupReadDTO();
                            gd.setName(g.getName());
                            gd.setOptions(
                                    g.getOptions().stream()
                                            .sorted(Comparator.comparing(ProductOption::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                                    .thenComparing(ProductOption::getId))
                                            .map(o -> {
                                                OptionReadDTO od = new OptionReadDTO();
                                                od.setName(o.getName());
                                                od.setValue(o.getValue());
                                                od.setExtraPrice(o.getExtraPrice());
                                                od.setSign(o.getSign() != null ? o.getSign().name() : null);
                                                od.setSortOrder(o.getSortOrder());
                                                return od;
                                            })
                                            .collect(Collectors.toList())
                            );
                            return gd;
                        })
                        .collect(Collectors.toList())
        );

        // ✅ 추가입력필드 (누락 보완)
        dto.setExtraFields(
                productExtraFieldRepository.findByProductId(productId).stream()
                        .sorted(Comparator.comparing(ProductExtraField::getId)) // 필요 시 sortOrder 컬럼으로 변경
                        .map(f -> {
                            ExtraFieldReadDTO ed = new ExtraFieldReadDTO();
                            ed.setLabel(f.getLabel());
                            ed.setValue(f.getValue());
                            return ed;
                        })
                        .collect(Collectors.toList())
        );

        // 관련상품
        dto.setRelatedProducts(
                relatedProductRepository.findByBaseProductWithProduct(productId).stream()
                        .map(r -> {
                            RelatedProductReadDTO rd = new RelatedProductReadDTO();
                            rd.setId(r.getRelatedProduct().getId());
                            rd.setName(r.getRelatedProduct().getName());
                            rd.setSortOrder(r.getSortOrder());
                            rd.setType(r.getType() != null ? r.getType().name() : null);
                            return rd;
                        })
                        .collect(Collectors.toList())
        );

        // 번들
        dto.setBundleProducts(
                productBundleItemRepository.findByMainProductWithProduct(productId).stream()
                        .map(b -> {
                            BundleProductReadDTO bd = new BundleProductReadDTO();
                            bd.setId(b.getBundleProduct().getId());
                            bd.setName(b.getBundleProduct().getName());
                            bd.setSortOrder(b.getSortOrder());
                            return bd;
                        })
                        .collect(Collectors.toList())
        );

        // 딜러 등급별 추가할인
        dto.setDealerDiscounts(
                productGradeBenefitRepository.findByProductId(productId).stream()
                        .collect(Collectors.toMap(
                                gb -> gb.getDealerGrade().name(),
                                ProductGradeBenefit::getDiscountRate,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ))
        );

        // 프로모션
        dto.setDiscounts(
                productPromotionMappingRepository.findByProductWithPromotion(productId).stream()
                        .map(ProductPromotionMapping::getPromotion)
                        .map(this::toPromotionReadDTO)
                        .collect(Collectors.toList())
        );

        // 외부 카테고리(대/중/소 경로)
        List<SmallProductCategory> spcs = smallProductCategoryRepository.findByProductWithSmall(productId);
        dto.setExternalCategories(
                spcs.stream().map(spc -> buildCategoryPath(spc.getSmall()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        return dto;
    }

    private PromotionReadDTO toPromotionReadDTO(com.dev.IbioScience.model.product.Promotion pr) {
        PromotionReadDTO d = new PromotionReadDTO();
        d.setId(pr.getId());
        d.setName(pr.getName());
        d.setType(pr.getType() != null ? pr.getType().name() : null);
        d.setTerm(pr.getTerm() != null ? pr.getTerm().name() : null);
        d.setActive(Boolean.TRUE.equals(pr.getActive()));
        d.setStartDate(pr.getStartDate() != null ? pr.getStartDate().format(D) : null);
        d.setEndDate(pr.getEndDate() != null ? pr.getEndDate().format(D) : null);
        d.setTarget(pr.getTarget() != null ? pr.getTarget().name() : null);
        d.setTypeLabel(typeLabel(pr.getType()));
        d.setTermLabel(termLabel(pr.getTerm()));
        return d;
    }

    private String typeLabel(Enum<?> e) {
        if (e == null) return null;
        switch (e.name()) {
            case "DISCOUNT": return "할인";
            case "GIFT": return "증정";
            case "ONE_PLUS_ONE": return "1+1";
            case "COUPON": return "쿠폰";
            default: return e.name();
        }
    }

    private String termLabel(Enum<?> e) {
        if (e == null) return null;
        switch (e.name()) {
            case "PERIOD": return "기간";
            case "ALWAYS": return "상시";
            default: return e.name();
        }
    }

    private CategoryPathReadDTO buildCategoryPath(CategorySmall small) {
        // 소분류가 여러 중분류에 매핑될 수 있으므로 정렬상 우선 1건 선택
        List<MediumSmallCategory> paths = mediumSmallCategoryRepository.findPathsBySmall(small.getId());
        if (paths.isEmpty()) return null;
        MediumSmallCategory msc = paths.get(0);
        CategoryPathReadDTO c = new CategoryPathReadDTO();
        c.setLargeId(msc.getMedium().getLarge().getId());
        c.setLargeName(msc.getMedium().getLarge().getName());
        c.setMediumId(msc.getMedium().getId());
        c.setMediumName(msc.getMedium().getName());
        c.setSmallId(small.getId());
        c.setSmallName(small.getName());
        return c;
    }
}
