package com.dev.IbioScience.model.product.dealer;

import java.math.BigDecimal;

import com.dev.IbioScience.enums.product.PriceSign;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_product_option",
    indexes = {
        @Index(name = "ix_dealer_option_group", columnList = "dealer_product_option_group_id"),
        @Index(name = "ix_dealer_option_sort", columnList = "sort_order")
    }
)
public class DealerProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 옵션그룹 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_option_group_id", nullable = false)
    private DealerProductOptionGroup group;

    /** 옵션명 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 옵션값(내부값) */
    @Column(name = "value", length = 200)
    private String value;

    /** 추가금액 */
    @Column(name = "extra_price", precision = 12, scale = 2)
    private BigDecimal extraPrice;

    /** 금액부호 */
    @Enumerated(EnumType.STRING)
    @Column(name = "sign", length = 20)
    private PriceSign sign;

    /** 정렬순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;
}