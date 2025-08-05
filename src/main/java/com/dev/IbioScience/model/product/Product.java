package com.dev.IbioScience.model.product;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.enums.DisplayStatus;
import com.dev.IbioScience.model.product.enums.PriceExposeTarget;
import com.dev.IbioScience.model.product.enums.ProductNewState;
import com.dev.IbioScience.model.product.enums.ProductState;
import com.dev.IbioScience.model.product.enums.RelatedRegisterType;
import com.dev.IbioScience.model.product.enums.SaleStatus;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
    @Column(nullable = false, length = 200)
    private String name;

    // 품목코드(중복불가, 필수)
    @Column(nullable = false, unique = true, length = 100)
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

    // 제조사명(텍스트, FK아님)
    @Column(length = 255)
    private String manufacturerText; // 제조사명(텍스트)

    // 공급사명(텍스트, FK아님)
    @Column(length = 255)
    private String supplierText; // 공급사명(텍스트)

    // 브랜드 (FK 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @JsonBackReference("product-brand")
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

    // 내부 전용 자체 소분류 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_category_small_id")
    @JsonBackReference("product-internal-category")
    private InternalCategorySmall internalCategorySmall; // 내부 전용 소분류

    // 상품 신상상태(신상품/재고상품/전시상품) - 기본:신상품
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductNewState newState = ProductNewState.NEW;

    // 상품 요약설명 (input)
    @Column(length = 255)
    private String summaryDescription;

    // 상품 간략설명 (textarea)
    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    // 자체 상품코드 (input)
    @Column(length = 100)
    private String internalProductCode;

    // 소비자가격
    private Integer consumerPrice;

    // 판매가격
    private Integer salePrice;

    // 판매가격 노출 대상(회원/비회원)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceExposeTarget priceExposeTarget = PriceExposeTarget.MEMBER;

    // 판매가 대체문구 사용여부
    @Column(nullable = false)
    private Boolean usePriceReplacementText = false;

    // 판매가 대체문구
    @Column(length = 255)
    private String priceReplacementText;

    // 적립금 (%) (소수점 가능)
    private Float rewardRate;

    // 유효기간 시작일
    private LocalDate validFrom;

    // 유효기간 종료일
    private LocalDate validTo;

    // 대표/추가 이미지 리스트 (1:N)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // 상세페이지 이미지 리스트 (1:N)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDetailImage> detailImages = new ArrayList<>();

    // 옵션 그룹 리스트 (1:N)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOptionGroup> optionGroups = new ArrayList<>();

    // 추가 입력 필드 리스트 (1:N)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductExtraField> extraFields = new ArrayList<>();

    // 번들 아이템 리스트 (1:N)
    @OneToMany(mappedBy = "mainProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductBundleItem> bundleItems = new ArrayList<>();

    // 연관상품 리스트 (1:N)
    @OneToMany(mappedBy = "baseProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RelatedProduct> relatedProducts = new ArrayList<>();

    // 할인정책 N:N 매핑 리스트
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductPromotionMapping> discountMappings = new ArrayList<>();

    // 등급별 혜택 리스트
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductGradeBenefit> gradeBenefits = new ArrayList<>();
}