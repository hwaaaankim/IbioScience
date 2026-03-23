package com.dev.IbioScience.model.product.dealer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.auth.SellerDealerProfile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_product",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_dealer_product_seller_code",
            columnNames = {"seller_dealer_profile_id", "code"}
        )
    },
    indexes = {
        @Index(name = "ix_dealer_product_seller", columnList = "seller_dealer_profile_id"),
        @Index(name = "ix_dealer_product_code", columnList = "code"),
        @Index(name = "ix_dealer_product_state", columnList = "state"),
        @Index(name = "ix_dealer_product_display_status", columnList = "display_status"),
        @Index(name = "ix_dealer_product_sale_status", columnList = "sale_status"),
        @Index(name = "ix_dealer_product_created_at", columnList = "created_at")
    }
)
public class DealerProduct {

    /** 딜러상품 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 판매딜러 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_dealer_profile_id", nullable = false)
    private SellerDealerProfile sellerDealerProfile;

    /** 진열상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 30)
    private DisplayStatus displayStatus;

    /** 판매상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 30)
    private SaleStatus saleStatus;

    /** 상품명 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 품목코드
     * - 딜러별로 중복 불가
     * - 전체 플랫폼 유니크가 아니라 seller_dealer_profile_id + code 복합 유니크
     */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /** 판매수 */
    @Column(name = "sales_count", nullable = false)
    private Integer salesCount = 0;

    /** 조회수 */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    /** 등록일시 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 수정일시 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 상품상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private ProductState state = ProductState.NORMAL;

    /** 제조사명(텍스트) */
    @Column(name = "manufacturer_text", length = 255)
    private String manufacturerText;

    /** 공급사명(텍스트) */
    @Column(name = "supplier_text", length = 255)
    private String supplierText;

    /** 제조일자 */
    @Column(name = "manufactured_at")
    private LocalDate manufacturedAt;

    /** 공급일자/유통기한 */
    @Column(name = "expired_at")
    private LocalDate expiredAt;

    /** 상세설명 HTML */
    @Column(name = "detail_html", columnDefinition = "TEXT")
    private String detailHtml;

    /** 상품 신상상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false, length = 30)
    private ProductNewState newState = ProductNewState.NEW;

    /** 상품 요약설명 */
    @Column(name = "summary_description", length = 255)
    private String summaryDescription;

    /** 상품 간략설명 */
    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    /** 자체 상품코드 */
    @Column(name = "internal_product_code", length = 100)
    private String internalProductCode;

    /** 소비자가격 */
    @Column(name = "consumer_price")
    private Integer consumerPrice;

    /** 판매가격 */
    @Column(name = "sale_price")
    private Integer salePrice;

    /** 판매가격 노출대상 */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_expose_target", nullable = false, length = 30)
    private PriceExposeTarget priceExposeTarget = PriceExposeTarget.MEMBER;

    /** 판매가 대체문구 사용여부 */
    @Column(name = "use_price_replacement_text", nullable = false)
    private Boolean usePriceReplacementText = false;

    /** 판매가 대체문구 */
    @Column(name = "price_replacement_text", length = 255)
    private String priceReplacementText;

    /** 적립금 비율(%) */
    @Column(name = "reward_rate")
    private Float rewardRate;

    /** 유효기간 시작일 */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /** 유효기간 종료일 */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /** 아이콘 공개 URL */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /** 아이콘 저장 PATH */
    @Column(name = "icon_path", length = 500)
    private String iconPath;

    /** 아이콘 파일명 */
    @Column(name = "icon_file_name", length = 255)
    private String iconFileName;

    /** 아이콘 기간 사용여부 */
    @Column(name = "use_icon_period", nullable = false)
    private Boolean useIconPeriod = false;

    /** 아이콘 시작일 */
    @Column(name = "icon_start_date")
    private LocalDate iconStartDate;

    /** 아이콘 종료일 */
    @Column(name = "icon_end_date")
    private LocalDate iconEndDate;

    /** 카테고리(중-소-딜러상품) 매핑 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerMediumSmallProductCategory> categoryMappings = new ArrayList<>();

    /** 대표/추가 이미지 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductImage> images = new ArrayList<>();

    /** 상세 이미지 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductDetailImage> detailImages = new ArrayList<>();

    /** 옵션 그룹 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductOptionGroup> optionGroups = new ArrayList<>();

    /** 추가 입력 필드 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductExtraField> extraFields = new ArrayList<>();

    /** 키워드 매핑 */
    @OneToMany(mappedBy = "dealerProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductKeyword> keywordMappings = new ArrayList<>();
}