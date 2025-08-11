package com.dev.IbioScience.service.category;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.productRegister.ProductSimpleDTO;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
	
	private final SmallProductCategoryRepository spcRepo;
	private final ProductRepository productRepository;
	
	// 소분류 id로 연결된 제품목록(간단 정보) 반환
	@Transactional(readOnly = true)
    public List<ProductSimpleDTO> getSimpleProductListBySmallIdLegacy(Long smallId) {
        List<Product> products = spcRepo.findProductsBySmallCategoryId(smallId);
        return products.stream()
                .map(p -> new ProductSimpleDTO(p.getId(), p.getCode(), p.getName()))
                .toList();
    }

    /** 신규: largeId/mediumId/smallId/keyword 임의 조합 검색 */
    @Transactional(readOnly = true)
    public List<ProductSimpleDTO> searchSimpleProducts(Long largeId,
                                                       Long mediumId,
                                                       Long smallId,
                                                       String keyword,
                                                       Integer page,
                                                       Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 100 : size; // 기본 100건
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"));
        String kw = (keyword == null) ? null : keyword.trim();
        return productRepository.searchSimpleProducts(largeId, mediumId, smallId, kw, pageable);
    }
	
	public record ProductWithCategoryDto(
		    Long productId,
		    String productCode,
		    String productName,
		    String largeCategoryName,
		    String mediumCategoryName,
		    String smallCategoryName
		) {}
}