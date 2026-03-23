package com.dev.IbioScience.model.order;

import com.dev.IbioScience.enums.product.dealer.OrderItemProductType;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "tb_order_item",
    indexes = {
        @Index(name = "ix_order_item_order_id", columnList = "order_id"),
        @Index(name = "ix_order_item_product_id", columnList = "product_id"),
        @Index(name = "ix_order_item_dealer_product_id", columnList = "dealer_product_id"),
        @Index(name = "ix_order_item_product_type", columnList = "item_product_type")
    }
)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 주문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** 상품 출처 타입 */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_product_type", nullable = false, length = 20)
    private OrderItemProductType itemProductType;

    /** 우리 회사 상품(회사상품인 경우만 값 존재) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** 딜러상품(딜러상품인 경우만 값 존재) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id")
    private DealerProduct dealerProduct;

    /** 회사상품 옵션그룹 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_group_id")
    private ProductOptionGroup productOptionGroup;

    /** 회사상품 옵션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    /** 딜러상품 옵션그룹 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_option_group_id")
    private DealerProductOptionGroup dealerProductOptionGroup;

    /** 딜러상품 옵션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_option_id")
    private DealerProductOption dealerProductOption;

    // =========================
    // 스냅샷 문자열
    // =========================

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    @Column(name = "option_group_name", length = 200)
    private String optionGroupName;

    @Column(name = "option_name", length = 200)
    private String optionName;

    @Column(name = "option_code", length = 100)
    private String optionCode;

    @Column(name = "unit_text", length = 50)
    private String unitText;

    /** 단가(주문 당시) */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    /** 수량 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 라인금액 */
    @Column(name = "line_price", nullable = false)
    private Long linePrice;

    /** 라인 적립금 */
    @Column(name = "item_earn_point", nullable = false)
    private Long itemEarnPoint;

    @PrePersist
    @PreUpdate
    private void validateProductReference() {
        if (itemProductType == null) {
            throw new IllegalStateException("OrderItem.itemProductType 는 필수입니다.");
        }

        if (itemProductType == OrderItemProductType.COMPANY) {
            if (product == null) {
                throw new IllegalStateException("회사상품 주문은 product 가 반드시 있어야 합니다.");
            }
            if (dealerProduct != null) {
                throw new IllegalStateException("회사상품 주문에는 dealerProduct 가 있으면 안 됩니다.");
            }
            if (dealerProductOptionGroup != null || dealerProductOption != null) {
                throw new IllegalStateException("회사상품 주문에는 딜러 옵션 참조가 있으면 안 됩니다.");
            }
        }

        if (itemProductType == OrderItemProductType.DEALER) {
            if (dealerProduct == null) {
                throw new IllegalStateException("딜러상품 주문은 dealerProduct 가 반드시 있어야 합니다.");
            }
            if (product != null) {
                throw new IllegalStateException("딜러상품 주문에는 product 가 있으면 안 됩니다.");
            }
            if (productOptionGroup != null || productOption != null) {
                throw new IllegalStateException("딜러상품 주문에는 회사상품 옵션 참조가 있으면 안 됩니다.");
            }
        }
    }
}