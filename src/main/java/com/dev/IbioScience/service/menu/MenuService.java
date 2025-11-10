package com.dev.IbioScience.service.menu;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.page.index.BrandSimpleDTO;
import com.dev.IbioScience.dto.page.index.IdNameDTO;
import com.dev.IbioScience.dto.page.index.ProductSimpleDTO;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
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
     * - largeId / mediumId / smallId / brandId 중 선택적으로 전달
     * - 우선순위: smallId가 있으면 해당 소분류만, 없으면 mediumId들의 소분류 집합, 없으면 largeId의 모든 중분류→소분류 집합
     * - brandId가 있으면 제품의 brand.id 일치 필터
     */
    public List<ProductSimpleDTO> listProductsIntersect(Long largeId, Long mediumId, Long smallId, Long brandId) {

        // 1) small 후보군 도출
        Set<Long> smallIds = new LinkedHashSet<>();
        if (smallId != null) {
            smallIds.add(smallId);
        } else if (mediumId != null) {
            smallIds.addAll(mscRepository.findSmallsByMediumId(mediumId).stream().map(CategorySmall::getId).collect(Collectors.toList()));
        } else if (largeId != null) {
            smallIds.addAll(mscRepository.findSmallIdsByLargeId(largeId));
        } else {
            // 범위 파라미터가 전혀 없으면 빈 목록(안전)
            return Collections.emptyList();
        }
        if (smallIds.isEmpty()) return Collections.emptyList();

        // 2) 소분류→제품 매핑 조회
        List<SmallProductCategory> maps = spcRepository.findBySmallIdInOrderBySortOrderAscIdAsc(smallIds);

        // 3) 브랜드 필터 및 최소 정보 변환
        return maps.stream()
                .map(m -> {
                    Product p = m.getProduct();
                    Long bid = (p.getBrand() != null ? p.getBrand().getId() : null);
                    return new ProductSimpleDTO(p.getId(), p.getName(), bid);
                })
                .filter(dto -> brandId == null || Objects.equals(brandId, dto.getBrandId()))
                .collect(Collectors.toList());
    }
}