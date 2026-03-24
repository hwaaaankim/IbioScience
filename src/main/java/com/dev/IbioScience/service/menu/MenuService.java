package com.dev.IbioScience.service.menu;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.page.index.BrandSimpleDTO;
import com.dev.IbioScience.dto.page.index.IdNameDTO;
import com.dev.IbioScience.dto.page.index.ProductSimpleDTO;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.BrandRepository;
import com.dev.IbioScience.repository.product.dealer.DealerMediumSmallProductCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final DisplayStatus FRONT_DISPLAY_STATUS = DisplayStatus.ON;
    private static final SaleStatus FRONT_SALE_STATUS = SaleStatus.ON;
    private static final ProductState FRONT_PRODUCT_STATE = ProductState.NORMAL;

    private final BrandRepository brandRepository;
    private final CategoryLargeRepository largeRepository;
    private final CategoryMediumRepository mediumRepository;
    private final MediumSmallCategoryRepository mscRepository;
    private final SmallProductCategoryRepository spcRepository;
    private final MediumSmallProductCategoryRepository mspcRepository;

    private final DealerMediumSmallProductCategoryRepository dealerMspcRepository;

    /** 대분류 목록 */
    public List<IdNameDTO> listLarge() {
        return largeRepository.findAll().stream()
                .map(l -> new IdNameDTO(l.getId(), l.getName()))
                .collect(Collectors.toList());
    }

    /** 대분류 → 중분류(1:N) */
    public List<IdNameDTO> listMediumByLarge(Long largeId) {
        return mediumRepository.findByLargeIdOrderByNameAsc(largeId).stream()
                .map(m -> new IdNameDTO(m.getId(), m.getName()))
                .collect(Collectors.toList());
    }

    /** 중분류 → 소분류(N:N : MediumSmallCategory 경유) */
    public List<IdNameDTO> listSmallByMedium(Long mediumId) {
        return mscRepository.findSmallsByMediumId(mediumId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    /** 브랜드 전체 */
    public List<BrandSimpleDTO> listBrands() {
        return brandRepository.findAll().stream()
                .map(b -> new BrandSimpleDTO(b.getId(), b.getName(), b.getImageRoad()))
                .collect(Collectors.toList());
    }

    /**
     * 교집합 제품 조회
     *
     * - 회사상품 + 딜러상품 통합 반환
     * - 브랜드 선택 시 딜러상품은 제외 (브랜드가 없으므로 교집합 불가)
     * - 우선순위: (mediumId+smallId) > mediumId > largeId > smallId
     */
    public List<ProductSimpleDTO> listProductsIntersect(Long largeId, Long mediumId, Long smallId, Long brandId) {
        List<ProductSimpleDTO> companyProducts = findCompanyProducts(largeId, mediumId, smallId, brandId);
        companyProducts.forEach(ProductSimpleDTO::applyCompanyMetadata);

        List<ProductSimpleDTO> dealerProducts =
                (brandId == null)
                        ? findDealerProducts(largeId, mediumId, smallId)
                        : Collections.emptyList();

        Map<String, ProductSimpleDTO> merged = new LinkedHashMap<>();

        Stream.concat(companyProducts.stream(), dealerProducts.stream())
                .forEach(dto -> merged.put(dto.getProductKey(), dto));

        return merged.values().stream()
                .sorted(
                        Comparator.comparing(ProductSimpleDTO::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(dto -> dto.getProductSourceType().name())
                                .thenComparing(ProductSimpleDTO::getId)
                )
                .collect(Collectors.toList());
    }

    private List<ProductSimpleDTO> findCompanyProducts(Long largeId, Long mediumId, Long smallId, Long brandId) {
        if (mediumId != null && smallId != null) {
            return mspcRepository.findProductsByMediumAndSmall(mediumId, smallId, brandId);
        }

        if (mediumId != null) {
            return mspcRepository.findProductsByMedium(mediumId, brandId);
        }

        if (largeId != null) {
            List<CategoryMedium> mediums = mediumRepository.findByLargeId(largeId);
            if (mediums == null || mediums.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> mediumIds = mediums.stream()
                    .map(CategoryMedium::getId)
                    .toList();

            return mspcRepository.findProductsByMediumIds(mediumIds, brandId);
        }

        if (smallId != null) {
            return mspcRepository.findProductsBySmall(smallId, brandId);
        }

        return Collections.emptyList();
    }

    private List<ProductSimpleDTO> findDealerProducts(Long largeId, Long mediumId, Long smallId) {
        if (mediumId != null && smallId != null) {
            return dealerMspcRepository.findProductsByMediumAndSmall(
                            mediumId,
                            smallId,
                            FRONT_DISPLAY_STATUS,
                            FRONT_SALE_STATUS,
                            FRONT_PRODUCT_STATE
                    ).stream()
                    .map(p -> ProductSimpleDTO.fromDealer(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        }

        if (mediumId != null) {
            return dealerMspcRepository.findProductsByMedium(
                            mediumId,
                            FRONT_DISPLAY_STATUS,
                            FRONT_SALE_STATUS,
                            FRONT_PRODUCT_STATE
                    ).stream()
                    .map(p -> ProductSimpleDTO.fromDealer(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        }

        if (largeId != null) {
            List<CategoryMedium> mediums = mediumRepository.findByLargeId(largeId);
            if (mediums == null || mediums.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> mediumIds = mediums.stream()
                    .map(CategoryMedium::getId)
                    .toList();

            return dealerMspcRepository.findProductsByMediumIds(
                            mediumIds,
                            FRONT_DISPLAY_STATUS,
                            FRONT_SALE_STATUS,
                            FRONT_PRODUCT_STATE
                    ).stream()
                    .map(p -> ProductSimpleDTO.fromDealer(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        }

        if (smallId != null) {
            return dealerMspcRepository.findProductsBySmall(
                            smallId,
                            FRONT_DISPLAY_STATUS,
                            FRONT_SALE_STATUS,
                            FRONT_PRODUCT_STATE
                    ).stream()
                    .map(p -> ProductSimpleDTO.fromDealer(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}