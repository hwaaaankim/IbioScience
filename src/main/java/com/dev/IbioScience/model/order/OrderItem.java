package com.dev.IbioScience.model.order;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        @Index(name = "ix_order_item_product_id", columnList = "product_id")
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

    /** 어떤 상품인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 선택한 옵션그룹(선택: 옵션 없는 상품이면 null 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_group_id")
    private ProductOptionGroup productOptionGroup;

    /** 선택한 옵션(선택: 옵션 없는 상품이면 null 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

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

    /** 라인금액 = unitPrice * quantity */
    @Column(name = "line_price", nullable = false)
    private Long linePrice;

    /** ✅ 라인 적립금(주문 당시 계산값) */
    @Column(name = "item_earn_point", nullable = false)
    private Long itemEarnPoint;
}