package com.dev.IbioScience.service.product.front;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.front.productList.ProductListFilter;
import com.dev.IbioScience.dto.front.productList.ProductListRowDTO;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.PromotionType;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.InternalCategoryLarge;
import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductGradeBenefit;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.ProductPromotionMappingRepository;
import com.dev.IbioScience.repository.product.register.ProductGradeBenefitRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductListService {

	private final ProductRepository productRepository;
	private final ProductGradeBenefitRepository gradeBenefitRepository;
	private final ProductPromotionMappingRepository promotionMappingRepository;

	// 외부분류 요약(대표 1경로 + 외 N개) 생성을 위한 추가 리포지토리
	private final SmallProductCategoryRepository smallProductCategoryRepository;
	private final MediumSmallCategoryRepository mediumSmallCategoryRepository;
	private final CategorySmallRepository categorySmallRepository;
	private final CategoryMediumRepository categoryMediumRepository;
	private final CategoryLargeRepository categoryLargeRepository;

	public Page<ProductListRowDTO> search(ProductListFilter f) {
		int pageIndex = (f.getPage() == null || f.getPage() < 1) ? 0 : f.getPage() - 1;
		int size = (f.getSize() == null || f.getSize() <= 0) ? 10 : f.getSize();
		Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "id"));

		Specification<Product> spec = buildSpec(f);
		Page<Product> page = productRepository.findAll(spec, pageable);

		List<Product> products = page.getContent();
		List<Long> ids = products.stream().map(Product::getId).toList();

		// 1) 등급별 혜택 일괄 조회
		Map<Long, List<ProductGradeBenefit>> benefitMap = gradeBenefitRepository.findByProductIds(ids).stream()
				.collect(Collectors.groupingBy(b -> b.getProduct().getId()));

		// 2) 활성 프로모션 타입 일괄 조회
		Map<Long, Set<PromotionType>> promoMap = new HashMap<>();
		if (!ids.isEmpty()) {
			LocalDate today = LocalDate.now();
			for (Object[] row : promotionMappingRepository.findActivePromotionTypesByProductIds(ids, today)) {
				Long pid = (Long) row[0];
				PromotionType type = (PromotionType) row[1];
				promoMap.computeIfAbsent(pid, k -> new LinkedHashSet<>()).add(type);
			}
		}

		// === (3) 외부분류 요약(대표 1경로 + 외 N개) — 항상 생성 ===
		final Map<Long, String> externalSummaryMap = (!ids.isEmpty()) ? buildExternalCategorySummary(ids)
				: java.util.Collections.emptyMap();

		// === (4) DTO 매핑 ===
		java.util.List<ProductListRowDTO> rows = products.stream().map(p -> {
			java.util.Map<String, Integer> dealerPrices = buildDealerPrices(p, benefitMap.get(p.getId()));
			String externalPathSummary = externalSummaryMap.get(p.getId());

			return ProductListRowDTO.builder().id(p.getId()).internalProductCode(p.getInternalProductCode())
					.code(p.getCode())
					// categoryPath 는 리스트 표시용으로 더 이상 사용하지 않으니 null 유지
					.categoryPath(null).externalCategorySummary(externalPathSummary) // 항상 소비자용 외부분류 표시
					.imageUrl(resolveMainImageUrl(p)).name(p.getName()).consumerPrice(p.getConsumerPrice())
					.salePrice(p.getSalePrice()).dealerPrices(dealerPrices)
					.promotionTypes(promoMap.getOrDefault(p.getId(), java.util.Collections.emptySet())).build();
		}).collect(java.util.stream.Collectors.toList());

		return new PageImpl<>(rows, pageable, page.getTotalElements());
	}

	/** 동적 검색 조건 */
	/** 동적 검색 조건 */
	private Specification<Product> buildSpec(ProductListFilter f) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// 날짜(등록/수정): dateFrom / dateTo 만 사용
			if (f.getDateField() != null && !f.getDateField().isBlank()) {
				Path<LocalDateTime> datePath = "updatedAt".equalsIgnoreCase(f.getDateField()) ? root.get("updatedAt")
						: root.get("createdAt");

				LocalDate from = f.getDateFrom();
				LocalDate to = f.getDateTo();

				LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
				LocalDateTime toDt = (to != null) ? to.atTime(23, 59, 59) : null;

				if (fromDt != null && toDt != null) {
					predicates.add(cb.between(datePath, fromDt, toDt));
				} else if (fromDt != null) {
					// 시작일만 있으면 시작일부터 쭉~~
					predicates.add(cb.greaterThanOrEqualTo(datePath, fromDt));
				} else if (toDt != null) {
					// 종료일만 있으면 종료일까지 쭉~~
					predicates.add(cb.lessThanOrEqualTo(datePath, toDt));
				}
			}

			// 진열/판매 상태
			if (f.getDisplayStatus() != null && !f.getDisplayStatus().isBlank()) {
				predicates.add(cb.equal(root.get("displayStatus"), DisplayStatus.valueOf(f.getDisplayStatus())));
			}
			if (f.getSaleStatus() != null && !f.getSaleStatus().isBlank()) {
				predicates.add(cb.equal(root.get("saleStatus"), SaleStatus.valueOf(f.getSaleStatus())));
			}

			// 검색 (제품명 / 자체코드 / 품목코드 / 브랜드명)
			if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
				String kw = "%" + f.getKeyword().trim() + "%";
				if ("internalProductCode".equalsIgnoreCase(f.getSearchType())) {
					predicates.add(cb.like(root.get("internalProductCode"), kw));
				} else if ("code".equalsIgnoreCase(f.getSearchType())) {
					predicates.add(cb.like(root.get("code"), kw));
				} else if ("brand".equalsIgnoreCase(f.getSearchType())) {
					Join<Product, Brand> brand = root.join("brand", JoinType.LEFT);
					predicates.add(cb.like(brand.get("name"), kw));
				} else { // name
					predicates.add(cb.like(root.get("name"), kw));
				}
			}

			// 분류(내부/외부)
			if ("INTERNAL".equalsIgnoreCase(f.getCategoryMode())) {
				if (f.getSmallId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.LEFT);
					predicates.add(cb.equal(small.get("id"), f.getSmallId()));
				} else if (f.getMediumId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.LEFT);
					Join<InternalCategorySmall, InternalCategoryMedium> medium = small.join("medium", JoinType.LEFT);
					predicates.add(cb.equal(medium.get("id"), f.getMediumId()));
				} else if (f.getLargeId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.LEFT);
					Join<InternalCategorySmall, InternalCategoryMedium> medium = small.join("medium", JoinType.LEFT);
					Join<InternalCategoryMedium, InternalCategoryLarge> large = medium.join("large", JoinType.LEFT);
					predicates.add(cb.equal(large.get("id"), f.getLargeId()));
				}
			} else if ("EXTERNAL".equalsIgnoreCase(f.getCategoryMode())) {
				// 외부분류: N:N (product -> SmallProductCategory -> CategorySmall)
				if (f.getSmallId() != null || f.getMediumId() != null || f.getLargeId() != null) {
					Join<Product, SmallProductCategory> spc = root.join("smallProductCategories", JoinType.LEFT);
					Join<SmallProductCategory, CategorySmall> small = spc.join("small", JoinType.LEFT);

					if (f.getSmallId() != null) {
						predicates.add(cb.equal(small.get("id"), f.getSmallId()));
					} else {
						// Medium/Large 필터는 프론트에서 smallId 계산 후 전달하는 전략 유지
					}
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/** 외부분류: 대표 1경로 + 외 N개 요약 생성 */
	private Map<Long, String> buildExternalCategorySummary(Collection<Long> productIds) {
		Map<Long, String> result = new HashMap<>();
		if (productIds == null || productIds.isEmpty())
			return result;

		// 제품 → 소분류들 (N:N)
		List<SmallProductCategory> spcList = smallProductCategoryRepository.findByProductIds(productIds);
		Map<Long, Set<Long>> productToSmallIds = new HashMap<>();
		for (SmallProductCategory spc : spcList) {
			productToSmallIds.computeIfAbsent(spc.getProduct().getId(), k -> new LinkedHashSet<>())
					.add(spc.getSmall().getId());
		}
		Set<Long> allSmallIds = productToSmallIds.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
		if (allSmallIds.isEmpty()) {
			for (Long pid : productIds)
				result.put(pid, "-");
			return result;
		}

		// 소분류 → 중분류들 (N:N)
		List<MediumSmallCategory> mscList = mediumSmallCategoryRepository.findBySmallIds(allSmallIds);

		// 이름 맵 구성
		Set<Long> mediumIds = mscList.stream().map(msc -> msc.getMedium().getId()).collect(Collectors.toSet());
		List<CategorySmall> smalls = categorySmallRepository.findByIdIn(allSmallIds);
		List<CategoryMedium> mediums = categoryMediumRepository.findByIdIn(mediumIds);

		Map<Long, CategorySmall> smallMap = smalls.stream().collect(Collectors.toMap(CategorySmall::getId, s -> s));
		Map<Long, CategoryMedium> mediumMap = mediums.stream().collect(Collectors.toMap(CategoryMedium::getId, m -> m));

		// Large 맵
		Set<Long> largeIds = mediums.stream().map(m -> m.getLarge().getId()).collect(Collectors.toSet());
		List<CategoryLarge> larges = categoryLargeRepository.findByIdIn(largeIds);
		Map<Long, CategoryLarge> largeMap = larges.stream().collect(Collectors.toMap(CategoryLarge::getId, l -> l));

		// smallId -> msc 리스트 (정렬: sortOrder asc, null last → medium 이름 asc)
		Map<Long, List<MediumSmallCategory>> smallToMscs = mscList.stream()
				.collect(Collectors.groupingBy(msc -> msc.getSmall().getId()));
		smallToMscs.replaceAll((sid, list) -> list.stream().sorted((a, b) -> {
			Integer sa = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
			Integer sb = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
			int cmp = Integer.compare(sa, sb);
			if (cmp != 0)
				return cmp;
			String an = Optional.ofNullable(a.getMedium().getName()).orElse("");
			String bn = Optional.ofNullable(b.getMedium().getName()).orElse("");
			return an.compareToIgnoreCase(bn);
		}).toList());

		// 제품별 경로 조합 → 대표 1개 + 외 N개
		for (Long pid : productIds) {
			Set<Long> smallIds = productToSmallIds.getOrDefault(pid, Collections.emptySet());
			if (smallIds.isEmpty()) {
				result.put(pid, "-");
				continue;
			}

			List<String> paths = new ArrayList<>();
			for (Long sid : smallIds) {
				List<MediumSmallCategory> links = smallToMscs.getOrDefault(sid, Collections.emptyList());
				CategorySmall small = smallMap.get(sid);
				String sName = (small != null ? small.getName() : "-");

				if (links.isEmpty()) {
					paths.add("- > - > " + sName);
				} else {
					for (MediumSmallCategory link : links) {
						CategoryMedium m = mediumMap.get(link.getMedium().getId());
						String mName = (m != null ? m.getName() : "-");
						CategoryLarge l = (m != null ? largeMap.get(m.getLarge().getId()) : null);
						String lName = (l != null ? l.getName() : "-");
						paths.add(lName + " > " + mName + " > " + sName);
					}
				}
			}

			if (paths.isEmpty()) {
				result.put(pid, "-");
			} else {
				List<String> distinct = paths.stream().distinct().toList();
				String rep = distinct.get(0);
				int rest = distinct.size() - 1;
				result.put(pid, rest > 0 ? (rep + " 외 " + rest + "개") : rep);
			}
		}
		return result;
	}

	/** 대표 이미지 URL 선택 (MAIN 우선 → 그 외, sortOrder asc) */
	private String resolveMainImageUrl(Product p) {
		if (p == null || p.getImages() == null || p.getImages().isEmpty())
			return null;

		String main = p.getImages().stream().filter(img -> img.getType() == ProductImageType.MAIN)
				.filter(img -> img.getUrl() != null && !img.getUrl().isBlank())
				.sorted(Comparator.comparing(img -> Optional.ofNullable(img.getSortOrder()).orElse(Integer.MAX_VALUE)))
				.map(ProductImage::getUrl).findFirst().orElse(null);

		if (main != null)
			return main;

		return p.getImages().stream().filter(img -> img.getUrl() != null && !img.getUrl().isBlank())
				.sorted(Comparator.comparing(img -> Optional.ofNullable(img.getSortOrder()).orElse(Integer.MAX_VALUE)))
				.map(ProductImage::getUrl).findFirst().orElse(null);
	}

	/** 딜러가 계산 (등급별 할인 적용, 소수점 반올림) */
	private Map<String, Integer> buildDealerPrices(Product p, List<ProductGradeBenefit> benefits) {
		Map<String, Integer> result = new LinkedHashMap<>();
		Integer base = (p.getSalePrice() == null ? 0 : p.getSalePrice());

		for (DealerGrade grade : DealerGrade.values()) {
			BigDecimal rate = null;
			if (benefits != null) {
				for (ProductGradeBenefit b : benefits) {
					if (b.getDealerGrade() == grade && b.getDiscountRate() != null) {
						rate = b.getDiscountRate();
						break;
					}
				}
			}
			int price = base;
			if (rate != null) {
				BigDecimal hundred = new BigDecimal("100");
				BigDecimal discounted = new BigDecimal(base).multiply(hundred.subtract(rate)).divide(hundred);
				price = discounted.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
			}
			result.put(grade.name(), price);
		}
		return result;
	}
}