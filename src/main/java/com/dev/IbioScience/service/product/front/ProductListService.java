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
import java.util.Locale;
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
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.ProductPromotionMappingRepository;
import com.dev.IbioScience.repository.product.register.ProductGradeBenefitRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductListService {

	private final ProductRepository productRepository;
	private final ProductGradeBenefitRepository gradeBenefitRepository;
	private final ProductPromotionMappingRepository promotionMappingRepository;
	private final MediumSmallProductCategoryRepository mspcRepository;

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

		// 3) 모드별 분류 요약 생성 (표시용)
		String mode = normalizeMode(f.getCategoryMode());
		Map<Long, String> categorySummaryMap;
		if ("INTERNAL".equals(mode)) {
			categorySummaryMap = buildInternalCategorySummary(products);
		} else if ("EXTERNAL".equals(mode)) {
			categorySummaryMap = buildExternalCategorySummary(ids); // ✅ 새 테이블 기준
		} else {
			// 전체 모드면: 우선 외부가 있으면 외부요약, 없으면 내부요약, 둘다 없으면 "-"
			categorySummaryMap = buildMixedCategorySummary(products);
		}

		// 4) DTO 매핑
		List<ProductListRowDTO> rows = products.stream().map(p -> {
			Map<String, Integer> dealerPrices = buildDealerPrices(p, benefitMap.get(p.getId()));
			String categorySummary = categorySummaryMap.getOrDefault(p.getId(), "-");

			return ProductListRowDTO.builder().id(p.getId()).internalProductCode(p.getInternalProductCode())
					.code(p.getCode()).categorySummary(categorySummary).imageUrl(resolveMainImageUrl(p))
					.name(p.getName()).consumerPrice(p.getConsumerPrice()).salePrice(p.getSalePrice())
					.dealerPrices(dealerPrices).promotionTypes(promoMap.getOrDefault(p.getId(), Collections.emptySet()))
					.build();
		}).toList();

		return new PageImpl<>(rows, pageable, page.getTotalElements());
	}

	private String normalizeMode(String v) {
		return (v == null) ? "" : v.trim().toUpperCase(Locale.ROOT);
	}

	private Specification<Product> buildSpec(ProductListFilter f) {

		return (root, query, cb) -> {

			List<Predicate> predicates = new ArrayList<>();

			// ===== 날짜(등록/수정): dateFrom / dateTo =====
			if (f.getDateField() != null && !f.getDateField().isBlank()) {
				Path<LocalDateTime> datePath = "updatedAt".equalsIgnoreCase(f.getDateField()) ? root.get("updatedAt")
						: root.get("createdAt");

				LocalDate from = f.getDateFrom();
				LocalDate to = f.getDateTo();

				LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
				LocalDateTime toDt = (to != null) ? to.atTime(23, 59, 59) : null;

				if (fromDt != null && toDt != null)
					predicates.add(cb.between(datePath, fromDt, toDt));
				else if (fromDt != null)
					predicates.add(cb.greaterThanOrEqualTo(datePath, fromDt));
				else if (toDt != null)
					predicates.add(cb.lessThanOrEqualTo(datePath, toDt));
			}

			// ===== 진열/판매 상태 =====
			if (f.getDisplayStatus() != null && !f.getDisplayStatus().isBlank()) {
				predicates.add(cb.equal(root.get("displayStatus"), DisplayStatus.valueOf(f.getDisplayStatus())));
			}
			if (f.getSaleStatus() != null && !f.getSaleStatus().isBlank()) {
				predicates.add(cb.equal(root.get("saleStatus"), SaleStatus.valueOf(f.getSaleStatus())));
			}

			// ===== 검색 =====
			if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
				String kw = "%" + f.getKeyword().trim() + "%";

				if ("internalProductCode".equalsIgnoreCase(f.getSearchType())) {
					predicates.add(cb.like(root.get("internalProductCode"), kw));
				} else if ("code".equalsIgnoreCase(f.getSearchType())) {
					predicates.add(cb.like(root.get("code"), kw));
				} else if ("brand".equalsIgnoreCase(f.getSearchType())) {
					Join<Product, Brand> brand = root.join("brand", JoinType.LEFT);
					predicates.add(cb.like(brand.get("name"), kw));
				} else {
					predicates.add(cb.like(root.get("name"), kw));
				}
			}

			// ===== 분류(내부/외부) =====
			String mode = normalizeMode(f.getCategoryMode());

			if ("INTERNAL".equals(mode)) {

				// ✅ 핵심: INTERNAL 모드면 내부 분류가 없는 상품은 제외
				predicates.add(cb.isNotNull(root.get("internalCategorySmall")));

				// 필터가 있으면 더 좁힘 (INNER로 타고 들어가도 안전)
				if (f.getSmallId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.INNER);
					predicates.add(cb.equal(small.get("id"), f.getSmallId()));

				} else if (f.getMediumId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.INNER);
					Join<InternalCategorySmall, InternalCategoryMedium> medium = small.join("medium", JoinType.INNER);
					predicates.add(cb.equal(medium.get("id"), f.getMediumId()));

				} else if (f.getLargeId() != null) {
					Join<Product, InternalCategorySmall> small = root.join("internalCategorySmall", JoinType.INNER);
					Join<InternalCategorySmall, InternalCategoryMedium> medium = small.join("medium", JoinType.INNER);
					Join<InternalCategoryMedium, InternalCategoryLarge> large = medium.join("large", JoinType.INNER);
					predicates.add(cb.equal(large.get("id"), f.getLargeId()));
				}

			} else if ("EXTERNAL".equals(mode)) {

				// ✅ EXTERNAL: 새 경로 테이블 기준으로만 필터
				query.distinct(true);

				Subquery<Integer> sq = query.subquery(Integer.class);
				Root<MediumSmallProductCategory> mspc = sq.from(MediumSmallProductCategory.class);

				List<Predicate> sqPreds = new ArrayList<>();

				sqPreds.add(cb.equal(mspc.get("product").get("id"), root.get("id")));

				if (f.getLargeId() != null) {
					sqPreds.add(cb.equal(mspc.get("medium").get("large").get("id"), f.getLargeId()));
				}
				if (f.getMediumId() != null) {
					sqPreds.add(cb.equal(mspc.get("medium").get("id"), f.getMediumId()));
				}
				if (f.getSmallId() != null) {
					sqPreds.add(cb.equal(mspc.get("small").get("id"), f.getSmallId()));
				}

				sq.select(cb.literal(1)).where(cb.and(sqPreds.toArray(new Predicate[0])));
				predicates.add(cb.exists(sq));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	// =========================
	// 화면 표시용 "내부분류 요약"
	// =========================
	private Map<Long, String> buildInternalCategorySummary(List<Product> products) {
		Map<Long, String> map = new HashMap<>();
		for (Product p : products) {
			InternalCategorySmall s = p.getInternalCategorySmall();
			if (s == null) {
				map.put(p.getId(), "-");
				continue;
			}
			InternalCategoryMedium m = s.getMedium();
			InternalCategoryLarge l = (m != null) ? m.getLarge() : null;

			String lName = (l != null && l.getName() != null) ? l.getName() : "-";
			String mName = (m != null && m.getName() != null) ? m.getName() : "-";
			String sName = (s.getName() != null) ? s.getName() : "-";

			map.put(p.getId(), lName + " > " + mName + " > " + sName);
		}
		return map;
	}

	// =========================
	// 화면 표시용 "외부분류 요약" (✅ 새 테이블 기준)
	// =========================
	private Map<Long, String> buildExternalCategorySummary(Collection<Long> productIds) {
		Map<Long, String> result = new HashMap<>();
		if (productIds == null || productIds.isEmpty())
			return result;

		List<MediumSmallProductCategory> links = mspcRepository.findByProduct_IdIn(productIds);

		Map<Long, List<MediumSmallProductCategory>> byProduct = links.stream()
				.collect(Collectors.groupingBy(x -> x.getProduct().getId()));

		for (Long pid : productIds) {
			List<MediumSmallProductCategory> list = byProduct.getOrDefault(pid, Collections.emptyList());
			if (list.isEmpty()) {
				result.put(pid, "-");
				continue;
			}

			// 경로 문자열들(중복 제거) 생성
			List<String> paths = list.stream().map(x -> {
				String lName = (x.getMedium() != null && x.getMedium().getLarge() != null)
						? nz(x.getMedium().getLarge().getName())
						: "-";
				String mName = (x.getMedium() != null) ? nz(x.getMedium().getName()) : "-";
				String sName = (x.getSmall() != null) ? nz(x.getSmall().getName()) : "-";
				return lName + " > " + mName + " > " + sName;
			}).distinct().sorted().toList();

			String rep = paths.get(0);
			int rest = paths.size() - 1;
			result.put(pid, rest > 0 ? (rep + " 외 " + rest + "개") : rep);
		}

		return result;
	}

	private Map<Long, String> buildMixedCategorySummary(List<Product> products) {
		List<Long> ids = products.stream().map(Product::getId).toList();

		Map<Long, String> ext = buildExternalCategorySummary(ids);
		Map<Long, String> in = buildInternalCategorySummary(products);

		Map<Long, String> out = new HashMap<>();
		for (Product p : products) {
			String e = ext.get(p.getId());
			if (e != null && !e.equals("-"))
				out.put(p.getId(), e);
			else
				out.put(p.getId(), in.getOrDefault(p.getId(), "-"));
		}
		return out;
	}

	private String nz(String v) {
		return (v == null || v.isBlank()) ? "-" : v;
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