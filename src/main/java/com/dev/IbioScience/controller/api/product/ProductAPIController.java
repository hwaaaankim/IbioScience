package com.dev.IbioScience.controller.api.product;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.front.productList.ProductListFilter;
import com.dev.IbioScience.dto.front.productList.ProductListRowDTO;
import com.dev.IbioScience.dto.productDetail.ProductDetailReadResponseDTO;
import com.dev.IbioScience.dto.productDetail.ProductUpdateRequestDTO;
import com.dev.IbioScience.dto.productRegister.ProductRegisterMoveEditorImageRequestDTO;
import com.dev.IbioScience.dto.productRegister.ProductRegisterRequestDTO;
import com.dev.IbioScience.dto.productRegister.ProductSimpleDTO;
import com.dev.IbioScience.service.category.ProductService;
import com.dev.IbioScience.service.product.ProductDetailQueryService;
import com.dev.IbioScience.service.product.ProductRegisterService;
import com.dev.IbioScience.service.product.ProductUpdateService;
import com.dev.IbioScience.service.product.front.ProductListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductAPIController {

	private final ProductService productService;
	private final ProductRegisterService productRegisterService;
	private final ProductListService productListService;
	private final ProductDetailQueryService productDetailQueryService;
	private final ProductUpdateService productUpdateService;

	@GetMapping("/{id}/detail")
	public ResponseEntity<ProductDetailReadResponseDTO> getDetail(@PathVariable Long id) {
		return ResponseEntity.ok(productDetailQueryService.getDetail(id));
	}

	@GetMapping("/list-simple")
	public List<ProductSimpleDTO> listSimple(@RequestParam(required = false) Long largeId,
			@RequestParam(required = false) Long mediumId, @RequestParam(required = false) Long smallId,
			@RequestParam(required = false) String keyword, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		// 완전 하위호환: smallId만 들어온 경우 기존 로직도 그대로 지원
		if (largeId == null && mediumId == null && keyword == null && smallId != null && page == null && size == null) {
			return productService.getSimpleProductListBySmallIdLegacy(smallId);
		}
		return productService.searchSimpleProducts(largeId, mediumId, smallId, keyword, page, size);
	}

	@GetMapping("/list")
	@ResponseBody
	public Page<ProductListRowDTO> list(ProductListFilter filter) {
		return productListService.search(filter);
	}

	@PostMapping(value = "/editor-images", consumes = "multipart/form-data")
	public ResponseEntity<?> uploadEditorImages(@RequestParam("files") List<MultipartFile> files,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "key", required = false) String key) {
		System.out.println("/editor-images");
		List<String> urlList = productRegisterService.uploadEditorImages(files, type, key);
		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("imageUrls", urlList);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/{productId}/move-editor-images")
	public ResponseEntity<?> moveEditorImages(@PathVariable Long productId,
			@RequestBody ProductRegisterMoveEditorImageRequestDTO request) {
		System.out.println(request.getTempImgList().toString());
		String newHtml = productRegisterService.moveEditorImages(productId, request.getType(), request.getKey(),
				request.getHtml(), request.getTempImgList());
		return ResponseEntity.ok(Map.of("success", true, "newHtml", newHtml));
	}

	@PostMapping(value = "/insert", consumes = { "multipart/form-data" })
	public ResponseEntity<?> registerProduct(@RequestParam MultiValueMap<String, String> params,
			@RequestParam(required = false) MultiValueMap<String, MultipartFile> files) throws IOException {

		ProductRegisterRequestDTO dto = mapToRegisterRequestDTO(params, files);
		Long productId = productRegisterService.registerProduct(dto);
		return ResponseEntity.ok(Map.of("success", true, "productId", productId));
	}

	private ProductRegisterRequestDTO mapToRegisterRequestDTO(MultiValueMap<String, String> params,
			MultiValueMap<String, MultipartFile> files) {
		ProductRegisterRequestDTO dto = new ProductRegisterRequestDTO();

		dto.setProductName(nvl(params.getFirst("productName")));
		dto.setProductCode(nvl(params.getFirst("productCode")));
		dto.setDisplayStatus(nvl(params.getFirst("displayStatus")));
		dto.setSaleStatus(nvl(params.getFirst("saleStatus")));
		dto.setDetailHtml(nvl(params.getFirst("detailHtml")));

		List<String> smalls = params.get("categorySmallIds[]");
		if (smalls != null) {
			for (String v : smalls) {
				if (notEmpty(v))
					dto.getCategorySmallIds().add(Long.valueOf(v));
			}
		} else {
			for (int i = 0;; i++) {
				String v = params.getFirst("categorySmallIds[" + i + "]");
				if (v == null)
					break;
				if (notEmpty(v))
					dto.getCategorySmallIds().add(Long.valueOf(v));
			}
		}

		if (files != null) {
			MultipartFile main = getFirstFile(files, "mainImage");
			if (main != null && !main.isEmpty())
				dto.setMainImage(main);
			List<MultipartFile> subs = files.get("subImages[]");
			if (subs != null)
				dto.getSubImages().addAll(subs);
			for (int i = 0;; i++) {
				MultipartFile f = getFirstFile(files, "subImages[" + i + "]");
				if (f == null)
					break;
				dto.getSubImages().add(f);
			}
		}

		dto.setManufacturerText(nvl(params.getFirst("manufacturerText")));
		dto.setSupplierText(nvl(params.getFirst("supplierText")));
		String brandId = params.getFirst("brandId");
		if (notEmpty(brandId))
			dto.setBrandId(Long.valueOf(brandId));

		String manufacturedAt = params.getFirst("manufacturedAt");
		if (notEmpty(manufacturedAt))
			dto.setManufacturedAt(LocalDate.parse(manufacturedAt));
		String expiredAt = params.getFirst("expiredAt");
		if (notEmpty(expiredAt))
			dto.setExpiredAt(LocalDate.parse(expiredAt));

		dto.setSummaryDescription(nvl(params.getFirst("summaryDescription")));
		dto.setShortDescription(nvl(params.getFirst("shortDescription")));
		dto.setInternalProductCode(nvl(params.getFirst("internalProductCode")));

		String consumerPrice = params.getFirst("consumerPrice");
		if (notEmpty(consumerPrice))
			dto.setConsumerPrice(Integer.valueOf(consumerPrice));
		String salePrice = params.getFirst("salePrice");
		if (notEmpty(salePrice))
			dto.setSalePrice(Integer.valueOf(salePrice));

		dto.setPriceExposeTarget(nvl(params.getFirst("priceExposeTarget")));
		dto.setUsePriceReplacementText(toBool(params.getFirst("usePriceReplacementText")));
		dto.setPriceReplacementText(nvl(params.getFirst("priceReplacementText")));

		String rewardRate = params.getFirst("rewardRate");
		if (notEmpty(rewardRate))
			dto.setRewardRate(Float.valueOf(rewardRate));
		String validFrom = params.getFirst("validFrom");
		if (notEmpty(validFrom))
			dto.setValidFrom(LocalDate.parse(validFrom));
		String validTo = params.getFirst("validTo");
		if (notEmpty(validTo))
			dto.setValidTo(LocalDate.parse(validTo));

		dto.setUseRelatedProducts(Boolean.valueOf(nvl(params.getFirst("useRelatedProducts"), "false")));
		dto.setUseBundleItems(Boolean.valueOf(nvl(params.getFirst("useBundleItems"), "false")));

		String internalSmallId = params.getFirst("internalCategorySmallId");
		if (notEmpty(internalSmallId))
			dto.setInternalCategorySmallId(Long.valueOf(internalSmallId));

		String newState = params.getFirst("newState");
		if (notEmpty(newState))
			dto.setNewState(newState);

		if (files != null) {
			MultipartFile icon = getFirstFile(files, "iconImage");
			if (icon != null && !icon.isEmpty())
				dto.setIconImage(icon);
		}
		dto.setUseIconPeriod(Boolean.valueOf(nvl(params.getFirst("useIconPeriod"), "false")));
		String iconStart = params.getFirst("iconStartDate");
		if (notEmpty(iconStart))
			dto.setIconStartDate(LocalDate.parse(iconStart));
		else {
			String s = params.getFirst("icon-start");
			if (notEmpty(s))
				dto.setIconStartDate(LocalDate.parse(s));
		}
		String iconEnd = params.getFirst("iconEndDate");
		if (notEmpty(iconEnd))
			dto.setIconEndDate(LocalDate.parse(iconEnd));
		else {
			String e = params.getFirst("icon-end");
			if (notEmpty(e))
				dto.setIconEndDate(LocalDate.parse(e));
		}

		for (int i = 0;; i++) {
			String label = params.getFirst("extraFields[" + i + "].label");
			String value = params.getFirst("extraFields[" + i + "].value");
			if (label == null && value == null)
				break;
			ProductRegisterRequestDTO.ExtraFieldDTO ef = new ProductRegisterRequestDTO.ExtraFieldDTO();
			ef.setLabel(nvl(label));
			ef.setValue(nvl(value));
			dto.getExtraFields().add(ef);
		}

		for (int g = 0;; g++) {
			String gName = params.getFirst("optionGroups[" + g + "].name");
			if (gName == null)
				break;
			ProductRegisterRequestDTO.OptionGroupDTO og = new ProductRegisterRequestDTO.OptionGroupDTO();
			og.setName(nvl(gName));
			String gSort = params.getFirst("optionGroups[" + g + "].sortOrder");
			if (notEmpty(gSort))
				og.setSortOrder(Integer.valueOf(gSort));
			for (int o = 0;; o++) {
				String name = params.getFirst("optionGroups[" + g + "].options[" + o + "].name");
				if (name == null)
					break;
				ProductRegisterRequestDTO.OptionDTO opt = new ProductRegisterRequestDTO.OptionDTO();
				opt.setName(nvl(name));
				opt.setValue(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].value")));
				opt.setExtraPrice(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].extraPrice")));
				opt.setSign(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].sign")));
				String so = params.getFirst("optionGroups[" + g + "].options[" + o + "].sortOrder");
				if (notEmpty(so))
					opt.setSortOrder(Integer.valueOf(so));
				og.getOptions().add(opt);
			}
			dto.getOptionGroups().add(og);
		}

		List<String> kws = params.get("keywords[]");
		if (kws != null) {
			for (String kw : kws)
				if (notEmpty(kw))
					dto.getKeywords().add(kw);
		} else {
			for (int i = 0;; i++) {
				String kw = params.getFirst("keywords[" + i + "]");
				if (kw == null)
					break;
				if (notEmpty(kw))
					dto.getKeywords().add(kw);
			}
		}

		for (int i = 0;; i++) {
			String id = params.getFirst("relatedProducts[" + i + "].id");
			if (id == null)
				break;
			ProductRegisterRequestDTO.RelatedProductDTO rp = new ProductRegisterRequestDTO.RelatedProductDTO();
			rp.setId(Long.valueOf(id));
			rp.setType(nvl(params.getFirst("relatedProducts[" + i + "].type")));
			String so = params.getFirst("relatedProducts[" + i + "].sortOrder");
			if (notEmpty(so))
				rp.setSortOrder(Integer.valueOf(so));
			dto.getRelatedProducts().add(rp);
		}

		for (int i = 0;; i++) {
			String id = params.getFirst("bundleProducts[" + i + "].id");
			if (id == null)
				break;
			ProductRegisterRequestDTO.BundleProductDTO bp = new ProductRegisterRequestDTO.BundleProductDTO();
			bp.setId(Long.valueOf(id));
			String so = params.getFirst("bundleProducts[" + i + "].sortOrder");
			if (notEmpty(so))
				bp.setSortOrder(Integer.valueOf(so));
			dto.getBundleProducts().add(bp);
		}

		for (int i = 0;; i++) {
			String id = params.getFirst("discounts[" + i + "].id");
			if (id == null)
				break;
			ProductRegisterRequestDTO.DiscountDTO d = new ProductRegisterRequestDTO.DiscountDTO();
			d.setId(Long.valueOf(id));
			d.setName(nvl(params.getFirst("discounts[" + i + "].name")));
			d.setType(nvl(params.getFirst("discounts[" + i + "].type")));
			d.setTerm(nvl(params.getFirst("discounts[" + i + "].term")));
			d.setTarget(nvl(params.getFirst("discounts[" + i + "].target")));
			d.setCouponPolicy(nvl(params.getFirst("discounts[" + i + "].couponPolicy")));
			d.setStartDate(nvl(params.getFirst("discounts[" + i + "].startDate")));
			d.setEndDate(nvl(params.getFirst("discounts[" + i + "].endDate")));
			String active = params.getFirst("discounts[" + i + "].active");
			d.setActive("true".equalsIgnoreCase(active) || "on".equalsIgnoreCase(active) || "1".equals(active));
			dto.getDiscounts().add(d);
		}

		params.forEach((k, vs) -> {
			if (k != null && k.startsWith("dealerDiscounts[")) {
				String grade = k.replaceAll("^dealerDiscounts\\[(.+)\\]$", "$1");
				String v = nvl(params.getFirst(k));
				dto.getDealerDiscounts().put(grade, v);
			}
		});

		params.forEach((k, vs) -> {
			if (k != null && k.startsWith("question_")) {
				String v = nvl(params.getFirst(k));
				dto.getDisplayOptions().put(k, v);
			}
		});
		if (files != null) {
			files.forEach((k, vs) -> {
				if (k != null && k.startsWith("question_")) {
					MultipartFile f = getFirstFile(files, k);
					if (f != null)
						dto.getDisplayOptionFiles().put(k, f);
				}
			});
		}

		return dto;
	}

	private String nvl(String s) {
		return s == null ? "" : s;
	}

	private String nvl(String s, String def) {
		return s == null ? def : s;
	}

	private boolean notEmpty(String s) {
		return s != null && !s.isBlank();
	}

	private boolean toBool(String s) {
		return "true".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s) || "1".equals(s);
	}

	private MultipartFile getFirstFile(MultiValueMap<String, MultipartFile> files, String key) {
		if (files == null)
			return null;
		List<MultipartFile> list = files.get(key);
		if (list == null || list.isEmpty())
			return null;
		return list.get(0);
	}

	@PostMapping(value = "/{productId}/update", consumes = { "multipart/form-data" })
	public ResponseEntity<?> updateProduct(@PathVariable Long productId,
			@RequestParam MultiValueMap<String, String> params,
			@RequestParam(required = false) MultiValueMap<String, MultipartFile> files) throws IOException {

		ProductUpdateRequestDTO dto = mapToUpdateRequestDTO(params, files);
		productUpdateService.updateProduct(productId, dto);
		return ResponseEntity.ok(Map.of("success", true, "productId", productId));
	}

	private ProductUpdateRequestDTO mapToUpdateRequestDTO(MultiValueMap<String, String> params,
			MultiValueMap<String, MultipartFile> files) {

		ProductUpdateRequestDTO dto = new ProductUpdateRequestDTO();

		// ===== 기본 =====
		dto.setProductName(nvl(params.getFirst("productName")));
		dto.setProductCode(nvl(params.getFirst("productCode")));
		dto.setDisplayStatus(nvl(params.getFirst("displayStatus")));
		dto.setSaleStatus(nvl(params.getFirst("saleStatus")));
		dto.setDetailHtml(nvl(params.getFirst("detailHtml")));

		// 외부 카테고리
		List<String> smalls = params.get("categorySmallIds[]");
		if (smalls != null) {
			for (String v : smalls)
				if (notEmpty(v))
					dto.getCategorySmallIds().add(Long.valueOf(v));
		} else {
			for (int i = 0;; i++) {
				String v = params.getFirst("categorySmallIds[" + i + "]");
				if (v == null)
					break;
				if (notEmpty(v))
					dto.getCategorySmallIds().add(Long.valueOf(v));
			}
		}

		// 이미지 액션/파일
		dto.setMainImageAction(nvl(params.getFirst("mainImageAction"), "KEEP"));
		if (files != null) {
			MultipartFile main = getFirstFile(files, "mainImage");
			if (main != null && !main.isEmpty())
				dto.setMainImage(main);

			// 추가 이미지 신규
			List<MultipartFile> subs = files.get("subImages[]");
			if (subs != null)
				dto.getSubImages().addAll(subs);
			for (int i = 0;; i++) {
				MultipartFile f = getFirstFile(files, "subImages[" + i + "]");
				if (f == null)
					break;
				dto.getSubImages().add(f);
			}
		}
		// 추가 이미지 삭제대상(URL)
		List<String> dels = params.get("subImageDeleteUrls[]");
		if (dels != null) {
			for (String u : dels)
				if (notEmpty(u))
					dto.getSubImageDeleteUrls().add(u);
		} else {
			for (int i = 0;; i++) {
				String u = params.getFirst("subImageDeleteUrls[" + i + "]");
				if (u == null)
					break;
				if (notEmpty(u))
					dto.getSubImageDeleteUrls().add(u);
			}
		}

		// 제조/공급/브랜드/일자/요약
		dto.setManufacturerText(nvl(params.getFirst("manufacturerText")));
		dto.setSupplierText(nvl(params.getFirst("supplierText")));
		String brandId = params.getFirst("brandId");
		if (notEmpty(brandId))
			dto.setBrandId(Long.valueOf(brandId));

		String manufacturedAt = params.getFirst("manufacturedAt");
		if (notEmpty(manufacturedAt))
			dto.setManufacturedAt(LocalDate.parse(manufacturedAt));
		String expiredAt = params.getFirst("expiredAt");
		if (notEmpty(expiredAt))
			dto.setExpiredAt(LocalDate.parse(expiredAt));

		dto.setSummaryDescription(nvl(params.getFirst("summaryDescription")));
		dto.setShortDescription(nvl(params.getFirst("shortDescription")));
		dto.setInternalProductCode(nvl(params.getFirst("internalProductCode")));

		// 가격/정책
		String consumerPrice = params.getFirst("consumerPrice");
		if (notEmpty(consumerPrice))
			dto.setConsumerPrice(Integer.valueOf(consumerPrice));
		String salePrice = params.getFirst("salePrice");
		if (notEmpty(salePrice))
			dto.setSalePrice(Integer.valueOf(salePrice));

		dto.setPriceExposeTarget(nvl(params.getFirst("priceExposeTarget")));
		dto.setUsePriceReplacementText(toBool(params.getFirst("usePriceReplacementText")));
		dto.setPriceReplacementText(nvl(params.getFirst("priceReplacementText")));

		String rewardRate = params.getFirst("rewardRate");
		if (notEmpty(rewardRate))
			dto.setRewardRate(Float.valueOf(rewardRate));
		String validFrom = params.getFirst("validFrom");
		if (notEmpty(validFrom))
			dto.setValidFrom(LocalDate.parse(validFrom));
		String validTo = params.getFirst("validTo");
		if (notEmpty(validTo))
			dto.setValidTo(LocalDate.parse(validTo));

		dto.setUseRelatedProducts(Boolean.valueOf(nvl(params.getFirst("useRelatedProducts"), "false")));
		dto.setUseBundleItems(Boolean.valueOf(nvl(params.getFirst("useBundleItems"), "false")));

		String internalSmallId = params.getFirst("internalCategorySmallId");
		if (notEmpty(internalSmallId))
			dto.setInternalCategorySmallId(Long.valueOf(internalSmallId));

		String newState = params.getFirst("newState");
		if (notEmpty(newState))
			dto.setNewState(newState);

		// 아이콘
		dto.setIconImageAction(nvl(params.getFirst("iconImageAction"), "KEEP"));
		if (files != null) {
			MultipartFile icon = getFirstFile(files, "iconImage");
			if (icon != null && !icon.isEmpty())
				dto.setIconImage(icon);
		}
		dto.setUseIconPeriod(Boolean.valueOf(nvl(params.getFirst("useIconPeriod"), "false")));
		String iconStart = params.getFirst("iconStartDate");
		if (notEmpty(iconStart))
			dto.setIconStartDate(LocalDate.parse(iconStart));
		else {
			String s = params.getFirst("icon-start");
			if (notEmpty(s))
				dto.setIconStartDate(LocalDate.parse(s));
		}
		String iconEnd = params.getFirst("iconEndDate");
		if (notEmpty(iconEnd))
			dto.setIconEndDate(LocalDate.parse(iconEnd));
		else {
			String e = params.getFirst("icon-end");
			if (notEmpty(e))
				dto.setIconEndDate(LocalDate.parse(e));
		}

		// 추가 입력필드
		for (int i = 0;; i++) {
			String label = params.getFirst("extraFields[" + i + "].label");
			String value = params.getFirst("extraFields[" + i + "].value");
			if (label == null && value == null)
				break;
			ProductUpdateRequestDTO.ExtraFieldDTO ef = new ProductUpdateRequestDTO.ExtraFieldDTO();
			ef.setLabel(nvl(label));
			ef.setValue(nvl(value));
			dto.getExtraFields().add(ef);
		}

		// 옵션 그룹/옵션
		for (int g = 0;; g++) {
			String gName = params.getFirst("optionGroups[" + g + "].name");
			if (gName == null)
				break;
			ProductUpdateRequestDTO.OptionGroupDTO og = new ProductUpdateRequestDTO.OptionGroupDTO();
			og.setName(nvl(gName));
			String gSort = params.getFirst("optionGroups[" + g + "].sortOrder");
			if (notEmpty(gSort))
				og.setSortOrder(Integer.valueOf(gSort));
			for (int o = 0;; o++) {
				String name = params.getFirst("optionGroups[" + g + "].options[" + o + "].name");
				if (name == null)
					break;
				ProductUpdateRequestDTO.OptionDTO opt = new ProductUpdateRequestDTO.OptionDTO();
				opt.setName(nvl(name));
				opt.setValue(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].value")));
				opt.setExtraPrice(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].extraPrice")));
				opt.setSign(nvl(params.getFirst("optionGroups[" + g + "].options[" + o + "].sign")));
				String so = params.getFirst("optionGroups[" + g + "].options[" + o + "].sortOrder");
				if (notEmpty(so))
					opt.setSortOrder(Integer.valueOf(so));
				og.getOptions().add(opt);
			}
			dto.getOptionGroups().add(og);
		}

		// 키워드
		List<String> kws = params.get("keywords[]");
		if (kws != null) {
			for (String kw : kws)
				if (notEmpty(kw))
					dto.getKeywords().add(kw);
		} else {
			for (int i = 0;; i++) {
				String kw = params.getFirst("keywords[" + i + "]");
				if (kw == null)
					break;
				if (notEmpty(kw))
					dto.getKeywords().add(kw);
			}
		}

		// 관련상품
		for (int i = 0;; i++) {
			String id = params.getFirst("relatedProducts[" + i + "].id");
			if (id == null)
				break;
			ProductUpdateRequestDTO.RelatedProductDTO rp = new ProductUpdateRequestDTO.RelatedProductDTO();
			rp.setId(Long.valueOf(id));
			rp.setType(nvl(params.getFirst("relatedProducts[" + i + "].type")));
			String so = params.getFirst("relatedProducts[" + i + "].sortOrder");
			if (notEmpty(so))
				rp.setSortOrder(Integer.valueOf(so));
			dto.getRelatedProducts().add(rp);
		}

		// 번들상품
		for (int i = 0;; i++) {
			String id = params.getFirst("bundleProducts[" + i + "].id");
			if (id == null)
				break;
			ProductUpdateRequestDTO.BundleProductDTO bp = new ProductUpdateRequestDTO.BundleProductDTO();
			bp.setId(Long.valueOf(id));
			String so = params.getFirst("bundleProducts[" + i + "].sortOrder");
			if (notEmpty(so))
				bp.setSortOrder(Integer.valueOf(so));
			dto.getBundleProducts().add(bp);
		}

		// 프로모션
		for (int i = 0;; i++) {
			String id = params.getFirst("discounts[" + i + "].id");
			if (id == null)
				break;
			ProductUpdateRequestDTO.DiscountDTO d = new ProductUpdateRequestDTO.DiscountDTO();
			d.setId(Long.valueOf(id));
			d.setName(nvl(params.getFirst("discounts[" + i + "].name")));
			d.setType(nvl(params.getFirst("discounts[" + i + "].type")));
			d.setTerm(nvl(params.getFirst("discounts[" + i + "].term")));
			d.setTarget(nvl(params.getFirst("discounts[" + i + "].target")));
			d.setCouponPolicy(nvl(params.getFirst("discounts[" + i + "].couponPolicy")));
			d.setStartDate(nvl(params.getFirst("discounts[" + i + "].startDate")));
			d.setEndDate(nvl(params.getFirst("discounts[" + i + "].endDate")));
			String active = params.getFirst("discounts[" + i + "].active");
			d.setActive("true".equalsIgnoreCase(active) || "on".equalsIgnoreCase(active) || "1".equals(active));
			dto.getDiscounts().add(d);
		}

		// 딜러 할인율
		params.forEach((k, vs) -> {
			if (k != null && k.startsWith("dealerDiscounts[")) {
				String grade = k.replaceAll("^dealerDiscounts\\[(.+)\\]$", "$1");
				String v = nvl(params.getFirst(k));
				dto.getDealerDiscounts().put(grade, v);
			}
		});

		// 공통표시항목 값
		params.forEach((k, vs) -> {
			if (k != null && k.startsWith("question_") && !k.endsWith("_fileAction")) {
				String v = nvl(params.getFirst(k));
				dto.getDisplayOptions().put(k, v);
			}
		});
		// 파일형 질문 액션
		params.forEach((k, vs) -> {
			if (k != null && k.startsWith("question_") && k.endsWith("_fileAction")) {
				String v = nvl(params.getFirst(k));
				dto.getDisplayOptionFileActions().put(k, v);
			}
		});

		// 파일형 질문 업로드(복수 허용)
		if (files != null) {
			files.forEach((k, vs) -> {
				if (k != null && k.startsWith("question_") && !k.endsWith("_fileAction")) {
					List<MultipartFile> list = files.get(k);
					if (list != null && !list.isEmpty()) {
						dto.getDisplayOptionFiles().put(k, list);
					}
				}
			});
		}

		return dto;
	}
}
