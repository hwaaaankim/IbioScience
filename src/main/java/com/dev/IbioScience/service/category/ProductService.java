package com.dev.IbioScience.service.category;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
	private final SmallProductCategoryRepository spcRepo;

	// 소분류 id로 연결된 제품목록(간단 정보) 반환
	public List<ProductSimpleDto> getSimpleProductListBySmallId(Long smallId) {
		List<Product> products = spcRepo.findProductsBySmallCategoryId(smallId);
		return products.stream().map(p -> new ProductSimpleDto(p.getId(), p.getCode(), p.getName())).toList();
	}

	// DTO 정의
	public record ProductSimpleDto(Long id, String code, String name) {
	}
}