package com.dev.IbioScience.controller.customerPage;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;
import com.dev.IbioScience.dto.page.productList.ProductListItemDto;
import com.dev.IbioScience.enums.page.list.ProductSortOption;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.page.list.FrontProductListService;
import com.dev.IbioScience.service.product.front.ProductDetailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductFrontController {

	private final FrontProductListService productListService;
	private final ObjectMapper objectMapper;
	private final ProductDetailService productDetailService;

	@GetMapping("/productList")
	public String productList(@RequestParam(required = false) Long largeId,
			@RequestParam(required = false) Long mediumId, @RequestParam(required = false) Long smallId,
			@RequestParam(required = false) Long brandId, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size,
			@RequestParam(defaultValue = "CREATED_AT_DESC") ProductSortOption sort, Model model)
			throws JsonProcessingException {

		// keyword 공백 처리
		if (keyword != null) {
			keyword = keyword.trim();
			if (keyword.isEmpty()) {
				keyword = null;
			}
		}

		// ✅ 실제 조회 (페이지)
		Page<ProductListItemDto> productPage = productListService.searchProducts(largeId, mediumId, smallId, brandId,
				keyword, sort, page, size);

		// ✅ CATEGORY BEST (판매수 상위 10개)
		List<ProductListItemDto> categoryBestList = productListService.findCategoryBestProducts(largeId, mediumId,
				smallId, brandId, keyword);

		// ✅ breadcrumb / 좌측 필터용 텍스트
		String categoryPathText = buildCategoryPathText(largeId, mediumId, smallId);
		String brandLabel = buildBrandLabel(brandId, productPage);

		// 뷰로 전달
		model.addAttribute("productPage", productPage);
		model.addAttribute("categoryBestList", categoryBestList);

		// 현재 필터/정렬 상태
		model.addAttribute("largeId", largeId);
		model.addAttribute("mediumId", mediumId);
		model.addAttribute("smallId", smallId);
		model.addAttribute("brandId", brandId);
		model.addAttribute("keyword", keyword);

		model.addAttribute("page", page);
		model.addAttribute("size", size);
		model.addAttribute("sort", sort);
		model.addAttribute("sortName", sort.name());

		// 텍스트/카운트
		model.addAttribute("categoryPathText", categoryPathText);
		model.addAttribute("brandLabel", brandLabel);
		model.addAttribute("totalCount", productPage.getTotalElements());

		// JS에서 쓸 수 있도록 content 부분만 JSON 문자열로 변환 (필요시 사용)
		String productListJson = objectMapper.writeValueAsString(productPage.getContent());
		model.addAttribute("productListJson", productListJson);

		model.addAttribute("pageable", productPage.getPageable());

		return "front/product/productList";
	}

	/**
	 * breadcrumb / 좌측 카테고리용 간단 텍스트 - 실제 카테고리명 조회 서비스가 있다면 여기에서 ID 대신 이름으로 바꾸시면 됩니다.
	 */
	private String buildCategoryPathText(Long largeId, Long mediumId, Long smallId) {
		List<String> parts = new ArrayList<>();
		if (largeId != null)
			parts.add("대분류:" + largeId);
		if (mediumId != null)
			parts.add("중분류:" + mediumId);
		if (smallId != null)
			parts.add("소분류:" + smallId);

		if (parts.isEmpty()) {
			return "전체 분류";
		}
		return String.join(" > ", parts);
	}

	/**
	 * 브랜드 라벨 - 브랜드 ID만 있고 조회 결과가 없을 수 있으므로, 우선 페이지에서 브랜드명을 읽고, 없으면 "브랜드:ID" 형태로 표시
	 */
	private String buildBrandLabel(Long brandId, Page<ProductListItemDto> productPage) {
		if (brandId == null)
			return null;

		if (productPage != null && !productPage.isEmpty()) {
			ProductListItemDto first = productPage.getContent().get(0);
			if (first.getBrandName() != null) {
				return first.getBrandName();
			}
		}
		return "브랜드:" + brandId;
	}

	/**
	 * 제품 상세 페이지
	 *
	 * - /productDetail/{id} 필수 사용 권장
	 * - 잘못된 접근( id 없음 / 존재하지 않는 상품 / 진열 불가 상품 )은
	 *   메시지 출력 후 메인으로 redirect
	 */
	@GetMapping({"/productDetail", "/productDetail/{id}"})
	public String productDetail(@PathVariable(required = false) Long id,
	                            Model model,
	                            RedirectAttributes redirectAttributes,
	                            @AuthenticationPrincipal PrincipalDetails principal) {

	    // 0) id 없이 직접 /productDetail 접근한 경우
	    if (id == null) {
	        redirectAttributes.addFlashAttribute("errorMessage", "상품 정보가 존재하지 않습니다.");
	        return "redirect:/";
	    }

	    try {
	        // 1) 서비스에서 상세 DTO 조회 + 진열/상태 검사
	        ProductDetailResponseDto detail = productDetailService.getProductDetail(id);

	        // *** 여기서 디버깅용 전체 출력 ***
	        debugProductDetail(detail);

	        // 2) 로그인 회원 ID (리뷰 영역에서 "내 리뷰" 판단용)
	        Long loginMemberId = null;
	        if (principal != null && principal.getMember() != null) {
	            loginMemberId = principal.getMember().getId();
	        }

	        // 3) 뷰에서 사용할 데이터 model 에 세팅
	        model.addAttribute("productId", id);          // JS 등에서 사용
	        model.addAttribute("productDetail", detail);  // 상세 필드
	        model.addAttribute("loginMemberId", loginMemberId); // 내 리뷰 판단용

	        // 4) 상세 템플릿으로 이동
	        return "front/product/productDetail";

	    } catch (ProductNotDisplayableException e) {
	        log.warn("상품 상세 진입 차단 - 진열 불가 상품. id={}, message={}", id, e.getMessage());
	        redirectAttributes.addFlashAttribute(
	                "errorMessage",
	                e.getMessage() != null ? e.getMessage() : "진열하지 않는 상품입니다."
	        );
	        return "redirect:/";

	    } catch (ProductNotFoundException e) {
	        log.warn("상품 상세 진입 실패 - 상품 미존재. id={}", id);
	        redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 상품입니다.");
	        return "redirect:/";

	    } catch (Exception e) {
	        log.error("상품 상세 진입 중 알 수 없는 오류 발생. id={}", id, e);
	        redirectAttributes.addFlashAttribute("errorMessage", "상품 상세 정보를 불러오는 중 오류가 발생했습니다.");
	        return "redirect:/";
	    }
	}

	/**
	 * ProductDetailResponseDto 전체 디버깅 출력
	 */
	private void debugProductDetail(ProductDetailResponseDto detail) {
	    if (detail == null) {
	        System.out.println("=== ProductDetailResponseDto is NULL ===");
	        return;
	    }

	    System.out.println("==================================================");
	    System.out.println("=== ProductDetailResponseDto DEBUG START ========");
	    System.out.println("ID           : " + detail.getId());
	    System.out.println("Name         : " + detail.getName());
	    System.out.println("Code         : " + detail.getCode());
	    System.out.println("Display      : " + detail.getDisplayStatus() + " / " + detail.getDisplayStatusLabel());
	    System.out.println("SaleStatus   : " + detail.getSaleStatus() + " / " + detail.getSaleStatusLabel());
	    System.out.println("State        : " + detail.getProductState() + " / " + detail.getProductStateLabel());
	    System.out.println("NewState     : " + detail.getNewState() + " / " + detail.getNewStateLabel());
	    System.out.println("SalesCount   : " + detail.getSalesCount());
	    System.out.println("ViewCount    : " + detail.getViewCount());
	    System.out.println("CreatedAt    : " + detail.getCreatedAt());
	    System.out.println("UpdatedAt    : " + detail.getUpdatedAt());
	    System.out.println("Manufacturer : " + detail.getManufacturerText());
	    System.out.println("Supplier     : " + detail.getSupplierText());
	    System.out.println("Manufactured : " + detail.getManufacturedAt());
	    System.out.println("Expired      : " + detail.getExpiredAt());
	    System.out.println("SummaryDesc  : " + detail.getSummaryDescription());
	    System.out.println("ShortDesc    : " + detail.getShortDescription());
	    System.out.println("DetailHtml   : " + (detail.getDetailHtml() != null ? "[NOT NULL]" : "[NULL]"));
	    System.out.println("ConsumerPrice: " + detail.getConsumerPrice());
	    System.out.println("SalePrice    : " + detail.getSalePrice());
	    System.out.println("PriceTarget  : " + detail.getPriceExposeTarget());
	    System.out.println("UseReplaceTx : " + detail.getUsePriceReplacementText());
	    System.out.println("ReplaceText  : " + detail.getPriceReplacementText());
	    System.out.println("IconUrl      : " + detail.getIconUrl());
	    System.out.println("IconPath     : " + detail.getIconPath());
	    System.out.println("IconFileName : " + detail.getIconFileName());
	    System.out.println("UseIconPeriod: " + detail.getUseIconPeriod());
	    System.out.println("IconStart    : " + detail.getIconStartDate());
	    System.out.println("IconEnd      : " + detail.getIconEndDate());

	    // 브랜드
	    ProductDetailResponseDto.BrandDto brand = detail.getBrand();
	    if (brand != null) {
	        System.out.println("--- BRAND --------------------------------------");
	        System.out.println("Brand.id     : " + brand.getId());
	        System.out.println("Brand.name   : " + brand.getName());
	        System.out.println("Brand.image  : " + brand.getImageUrl());
	    } else {
	        System.out.println("Brand        : null");
	    }

	    // 카테고리 경로
	    System.out.println("--- CATEGORIES ---------------------------------");
	    if (detail.getCategories() != null) {
	        for (ProductDetailResponseDto.CategoryPathDto c : detail.getCategories()) {
	            System.out.println("  > L[" + c.getLargeId() + "," + c.getLargeName()
	                    + "] / M[" + c.getMediumId() + "," + c.getMediumName()
	                    + "] / S[" + c.getSmallId() + "," + c.getSmallName() + "]");
	        }
	    } else {
	        System.out.println("  (no categories)");
	    }

	    // 이미지
	    System.out.println("--- IMAGES -------------------------------------");
	    if (detail.getImages() != null) {
	        for (ProductDetailResponseDto.ProductImageDto img : detail.getImages()) {
	            System.out.println("  > IMG id=" + img.getId()
	                    + ", type=" + img.getType()
	                    + ", url=" + img.getUrl()
	                    + ", path=" + img.getPath()
	                    + ", fileName=" + img.getFileName()
	                    + ", sort=" + img.getSortOrder());
	        }
	    } else {
	        System.out.println("  (no images)");
	    }

	    // 상세 이미지
	    System.out.println("--- DETAIL IMAGES ------------------------------");
	    if (detail.getDetailImages() != null) {
	        for (ProductDetailResponseDto.ProductDetailImageDto di : detail.getDetailImages()) {
	            System.out.println("  > DIMG id=" + di.getId()
	                    + ", url=" + di.getUrl()
	                    + ", path=" + di.getPath()
	                    + ", fileName=" + di.getFileName()
	                    + ", orig=" + di.getOriginalFilename()
	                    + ", size=" + di.getSize()
	                    + ", inUse=" + di.getInUse()
	                    + ", sort=" + di.getSortOrder());
	        }
	    } else {
	        System.out.println("  (no detailImages)");
	    }

	    // 옵션 그룹
	    System.out.println("--- OPTION GROUPS ------------------------------");
	    if (detail.getOptionGroups() != null) {
	        for (ProductDetailResponseDto.OptionGroupDto g : detail.getOptionGroups()) {
	            System.out.println("  [GROUP] id=" + g.getId()
	                    + ", name=" + g.getName()
	                    + ", sort=" + g.getSortOrder());
	            if (g.getOptions() != null) {
	                for (ProductDetailResponseDto.OptionDto o : g.getOptions()) {
	                    System.out.println("    - OPT id=" + o.getId()
	                            + ", name=" + o.getName()
	                            + ", value=" + o.getValue()
	                            + ", extraPrice=" + o.getExtraPrice()
	                            + ", sign=" + o.getSign()
	                            + ", signLabel=" + o.getSignLabel()
	                            + ", sort=" + o.getSortOrder());
	                }
	            }
	        }
	    } else {
	        System.out.println("  (no optionGroups)");
	    }

	    // 추가필드
	    System.out.println("--- EXTRA FIELDS -------------------------------");
	    if (detail.getExtraFields() != null) {
	        for (ProductDetailResponseDto.ExtraFieldDto f : detail.getExtraFields()) {
	            System.out.println("  > Extra id=" + f.getId()
	                    + ", label=" + f.getLabel()
	                    + ", value=" + f.getValue());
	        }
	    } else {
	        System.out.println("  (no extraFields)");
	    }

	    // 번들
	    System.out.println("--- BUNDLE ITEMS -------------------------------");
	    if (detail.getBundleItems() != null) {
	        for (ProductDetailResponseDto.BundleItemDto b : detail.getBundleItems()) {
	            System.out.println("  > Bundle id=" + b.getId()
	                    + ", bundleProductId=" + b.getBundleProductId()
	                    + ", name=" + b.getBundleProductName()
	                    + ", sort=" + b.getSortOrder());
	        }
	    } else {
	        System.out.println("  (no bundleItems)");
	    }

	    // 연관상품
	    System.out.println("--- RELATED PRODUCTS ---------------------------");
	    if (detail.getRelatedProducts() != null) {
	        for (ProductDetailResponseDto.RelatedProductDto r : detail.getRelatedProducts()) {
	            System.out.println("  > Related id=" + r.getId()
	                    + ", relatedProductId=" + r.getRelatedProductId()
	                    + ", name=" + r.getRelatedProductName()
	                    + ", type=" + r.getRelatedType()
	                    + ", typeLabel=" + r.getRelatedTypeLabel()
	                    + ", sort=" + r.getSortOrder());
	        }
	    } else {
	        System.out.println("  (no relatedProducts)");
	    }

	    // 프로모션
	    System.out.println("--- PROMOTIONS ---------------------------------");
	    if (detail.getPromotions() != null) {
	        for (ProductDetailResponseDto.PromotionDto p : detail.getPromotions()) {
	            System.out.println("  > Promo id=" + p.getId()
	                    + ", name=" + p.getName()
	                    + ", type=" + p.getType() + "/" + p.getTypeLabel()
	                    + ", term=" + p.getTerm() + "/" + p.getTermLabel()
	                    + ", active=" + p.getActive()
	                    + ", discountPercent=" + p.getDiscountPercent()
	                    + ", couponName=" + p.getCouponName()
	                    + ", target=" + p.getTarget() + "/" + p.getTargetLabel());
	        }
	    } else {
	        System.out.println("  (no promotions)");
	    }

	    // 등급별 혜택
	    System.out.println("--- GRADE BENEFITS -----------------------------");
	    if (detail.getGradeBenefits() != null) {
	        for (ProductDetailResponseDto.GradeBenefitDto g : detail.getGradeBenefits()) {
	            System.out.println("  > GradeBenefit id=" + g.getId()
	                    + ", grade=" + g.getDealerGrade()
	                    + ", rate=" + g.getDiscountRate()
	                    + ", examplePrice=" + g.getExamplePrice());
	        }
	    } else {
	        System.out.println("  (no gradeBenefits)");
	    }

	    // 키워드
	    System.out.println("--- KEYWORDS -----------------------------------");
	    if (detail.getKeywords() != null) {
	        for (ProductDetailResponseDto.KeywordDto k : detail.getKeywords()) {
	            System.out.println("  > Keyword id=" + k.getId()
	                    + ", word=" + k.getWord());
	        }
	    } else {
	        System.out.println("  (no keywords)");
	    }

	    // 리뷰 요약
	    System.out.println("--- REVIEW SUMMARY -----------------------------");
	    ProductDetailResponseDto.ReviewSummaryDto rs = detail.getReviewSummary();
	    if (rs != null) {
	        System.out.println("  avgRating : " + rs.getAverageRating());
	        System.out.println("  reviewCnt : " + rs.getReviewCount());
	    } else {
	        System.out.println("  (no reviewSummary)");
	    }

	    // 리뷰
	    System.out.println("--- REVIEWS ------------------------------------");
	    if (detail.getReviews() != null) {
	        for (ProductDetailResponseDto.ReviewDto rv : detail.getReviews()) {
	            System.out.println("  > Review id=" + rv.getId()
	                    + ", memberId=" + rv.getMemberId()
	                    + ", name=" + rv.getMemberDisplayName()
	                    + ", rating=" + rv.getRating()
	                    + ", content=" + rv.getContent()
	                    + ", createdAt=" + rv.getCreatedAt());
	            if (rv.getImages() != null) {
	                for (ProductDetailResponseDto.ReviewImageDto ri : rv.getImages()) {
	                    System.out.println("    - img id=" + ri.getId()
	                            + ", url=" + ri.getUrl()
	                            + ", fileName=" + ri.getFileName()
	                            + ", sort=" + ri.getSortOrder());
	                }
	            }
	        }
	    } else {
	        System.out.println("  (no reviews)");
	    }

	    // 공통 질문/답변
	    System.out.println("--- QUESTION BLOCKS ----------------------------");
	    if (detail.getQuestions() != null) {
	        for (ProductDetailResponseDto.QuestionBlockDto q : detail.getQuestions()) {
	            System.out.println("  [Q] id=" + q.getQuestionId()
	                    + ", label=" + q.getLabel()
	                    + ", type=" + q.getType()
	                    + ", typeLabel=" + q.getTypeLabel()
	                    + ", required=" + q.getRequired()
	                    + ", sort=" + q.getSortOrder());

	            // 옵션
	            if (q.getOptions() != null) {
	                for (ProductDetailResponseDto.QuestionOptionDto opt : q.getOptions()) {
	                    System.out.println("    - OPT id=" + opt.getId()
	                            + ", value=" + opt.getValue()
	                            + ", sort=" + opt.getSortOrder());
	                }
	            }

	            // 답변
	            if (q.getAnswers() != null) {
	                for (ProductDetailResponseDto.AnswerDto a : q.getAnswers()) {
	                    System.out.println("    - ANS id=" + a.getId()
	                            + ", value=" + a.getValue()
	                            + ", fileUrl=" + a.getFileUrl()
	                            + ", path=" + a.getPath()
	                            + ", fileName=" + a.getFileName());
	                    if (a.getDetailImages() != null) {
	                        for (ProductDetailResponseDto.AnswerDetailImageDto adi : a.getDetailImages()) {
	                            System.out.println("       * IMG id=" + adi.getId()
	                                    + ", url=" + adi.getUrl()
	                                    + ", fileName=" + adi.getFileName()
	                                    + ", sort=" + adi.getSortOrder()
	                                    + ", inUse=" + adi.getInUse());
	                        }
	                    }
	                }
	            } else {
	                System.out.println("    (no answers)");
	            }
	        }
	    } else {
	        System.out.println("  (no questions)");
	    }

	    // 가격 예시
	    System.out.println("--- PRICE PREVIEW EXAMPLE ----------------------");
	    ProductDetailResponseDto.PricePreviewExampleDto pp = detail.getPricePreviewExample();
	    if (pp != null) {
	        System.out.println("  visibleGuest   : " + pp.isPriceVisibleForGuest());
	        System.out.println("  visibleMember  : " + pp.isPriceVisibleForMember());
	        System.out.println("  baseSalePrice  : " + pp.getBaseSalePrice());
	        System.out.println("  normalMember   : " + pp.getExampleNormalMemberPrice());
	        System.out.println("  dealerA        : " + pp.getExampleDealerAPrice());
	        System.out.println("  useReplText    : " + pp.isUseReplacementText());
	        System.out.println("  replText       : " + pp.getReplacementText());
	        System.out.println("  desc           : " + pp.getDescription());
	    } else {
	        System.out.println("  (no pricePreviewExample)");
	    }

	    System.out.println("=== ProductDetailResponseDto DEBUG END =========");
	    System.out.println("================================================");
	}

	
	
	@GetMapping("/dealerProductList")
	public String dealerProductList() {

		return "front/product/dealerProductList";
	}
}
