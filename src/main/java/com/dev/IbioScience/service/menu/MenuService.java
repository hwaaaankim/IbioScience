package com.dev.IbioScience.service.menu;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.page.index.BrandSimpleDTO;
import com.dev.IbioScience.dto.page.index.IdNameDTO;
import com.dev.IbioScience.dto.page.index.ProductSimpleDTO;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.BrandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final BrandRepository brandRepository;
    private final CategoryLargeRepository largeRepository;
    private final CategoryMediumRepository mediumRepository;
    private final MediumSmallCategoryRepository mscRepository;
    private final SmallProductCategoryRepository spcRepository;
    private final MediumSmallProductCategoryRepository mspcRepository;

    /** 대분류 목록 */
    public List<IdNameDTO> listLarge() {
        return largeRepository.findAll().stream()
                .map(l -> new IdNameDTO(l.getId(), l.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 대분류 → 중분류(1:N) */
    public List<IdNameDTO> listMediumByLarge(Long largeId) {
        return mediumRepository.findByLargeIdOrderByNameAsc(largeId).stream()
                .map(m -> new IdNameDTO(m.getId(), m.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 중분류 → 소분류(N:N : MediumSmallCategory 경유) */
    public List<IdNameDTO> listSmallByMedium(Long mediumId) {
        return mscRepository.findSmallsByMediumId(mediumId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 브랜드 전체 */
    public List<BrandSimpleDTO> listBrands() {
        return brandRepository.findAll().stream()
                .map(b -> new BrandSimpleDTO(b.getId(), b.getName(), b.getImageRoad()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * ✅ 교집합 제품 조회 (mspc 기준)
     * - 우선순위: (mediumId+smallId) > mediumId > largeId > smallId(호환)
     */
    public List<ProductSimpleDTO> listProductsIntersect(Long largeId, Long mediumId, Long smallId, Long brandId) {

        // 1) 가장 정확: medium+small
        if (mediumId != null && smallId != null) {
            return mspcRepository.findProductsByMediumAndSmall(mediumId, smallId, brandId);
        }

        // 2) medium만: 중분류 클릭
        if (mediumId != null) {
            return mspcRepository.findProductsByMedium(mediumId, brandId);
        }

        // 3) large만: 대분류 클릭 → large 하위 mediumIds
        if (largeId != null) {
            List<CategoryMedium> mediums = mediumRepository.findByLargeId(largeId);
            if (mediums == null || mediums.isEmpty()) return Collections.emptyList();

            List<Long> mediumIds = mediums.stream().map(CategoryMedium::getId).toList(); // (자바 17 이상이면 OK)
            return mspcRepository.findProductsByMediumIds(mediumIds, brandId);
        }

        // 4) small만(호환): old caller 방어
        if (smallId != null) {
            return mspcRepository.findProductsBySmall(smallId, brandId);
        }

        return Collections.emptyList();
    }
}