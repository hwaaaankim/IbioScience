package com.dev.IbioScience.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

// 상품 등록시 사용하는 DTO
@Data
public class ProductRegisterRequestDTO {
	private String productName;
	private String productCode;
	private String displayStatus;
	private String saleStatus;
	private String detailHtml;

	// 카테고리
	private List<Long> categorySmallIds = new ArrayList<>();

	// 이미지
	private MultipartFile mainImage;
	private List<MultipartFile> subImages = new ArrayList<>();

	// 추가 입력필드
	@Data
	public static class ExtraFieldDTO {
		private String label;
		private String value;
	}

	private List<ExtraFieldDTO> extraFields = new ArrayList<>();

	// 옵션그룹
	@Data
	public static class OptionDTO {
		private String name;
		private String value;
		private String extraPrice;
		private String sign;
		private Integer sortOrder;
	}

	@Data
	public static class OptionGroupDTO {
		private String name;
		private List<OptionDTO> options = new ArrayList<>();
	}

	private List<OptionGroupDTO> optionGroups = new ArrayList<>();

	// 키워드
	private List<String> keywords = new ArrayList<>();

	// 연관상품
	@Data
	public static class RelatedProductDTO {
		private Long id;
		private String type;
	}

	private List<RelatedProductDTO> relatedProducts = new ArrayList<>();

	// 할인혜택
	@Data
	public static class DiscountDTO {
		private Long id;
		private String name;
		private String type;
		private String term;
		private String target;
		private String couponPolicy;
		private String startDate;
		private String endDate;
		private Boolean active;
	}

	private List<DiscountDTO> discounts = new ArrayList<>();

	// 추가구성상품
	private List<Long> bundleProductIds = new ArrayList<>();

	// 딜러 등급별 할인
	private Map<String, String> dealerDiscounts = new HashMap<>();

	// 공통표시항목(질문/에디터)
	private Map<String, String> displayOptions = new HashMap<>();
	private Map<String, MultipartFile> displayOptionFiles = new HashMap<>();
}
