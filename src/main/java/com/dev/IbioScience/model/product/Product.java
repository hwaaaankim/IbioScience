package com.dev.IbioScience.model.product;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.status.DisplayStatus;
import com.dev.IbioScience.model.product.status.ProductState;
import com.dev.IbioScience.model.product.status.RelatedRegisterType;
import com.dev.IbioScience.model.product.status.SaleStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

//상품(제품) 엔티티
@Data
@Entity
@Table(name = "tb_product")
public class Product {
	
	// 제품 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 진열상태 (ON/OFF)
	@Enumerated(EnumType.STRING)
	private DisplayStatus displayStatus;

	// 판매상태 (ON/OFF)
	@Enumerated(EnumType.STRING)
	private SaleStatus saleStatus;

	// 상품명(필수)
	@Column(nullable = false)
	private String name;

	// 품목코드(중복불가, 필수)
	@Column(nullable = false, unique = true)
	private String code;

	// 판매수
	private Integer salesCount;
	
	// 조회수
	private Integer viewCount;

	// 등록일시
	private LocalDateTime createdAt;
	
	// 수정일시
	private LocalDateTime updatedAt;

	// 상품상태(정상/삭제대기/삭제)
	@Enumerated(EnumType.STRING)
	private ProductState state;

	// 제조사
	@ManyToOne(fetch = FetchType.LAZY)
	private Manufacturer manufacturer;
	
	// 공급사
	@ManyToOne(fetch = FetchType.LAZY)
	private Supplier supplier;
	
	// 브랜드
	@ManyToOne(fetch = FetchType.LAZY)
	private Brand brand;

	// 제조일자
	private LocalDate manufacturedAt;
	
	// 유통기한
	private LocalDate expiredAt;

	// CKEditor 등으로 작성된 상세설명(HTML)
	@Column(columnDefinition = "TEXT")
	private String detailHtml;

	// 추가구성상품 사용여부
	@Column(nullable = false)
	private Boolean useBundleItems = false;

	// 관련상품 사용여부
	@Column(nullable = false)
	private Boolean useRelatedProducts = false;

	// 관련상품 등록방식
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RelatedRegisterType relatedRegisterType = RelatedRegisterType.PRODUCT;
	
	// 대표/추가 이미지 리스트
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductImage> images = new ArrayList<>();

	// 상세페이지 이미지(업로드 전용, 본문=detailHtml)
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductDetailImage> detailImages = new ArrayList<>();

	// 옵션 그룹 리스트
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductOptionGroup> optionGroups = new ArrayList<>();

	// 추가 입력 필드 리스트
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductExtraField> extraFields = new ArrayList<>();

	// 번들 아이템(구성상품) 리스트
	@OneToMany(mappedBy = "mainProduct", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductBundleItem> bundleItems = new ArrayList<>();

	// 연관상품 리스트
	@OneToMany(mappedBy = "baseProduct", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RelatedProduct> relatedProducts = new ArrayList<>();
}
