package com.dev.IbioScience.model.product.dealer;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_product_extra_field",
    indexes = {
        @Index(name = "ix_dealer_extra_field_product", columnList = "dealer_product_id")
    }
)
public class DealerProductExtraField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 질문명 */
    @Column(name = "label", nullable = false, length = 200)
    private String label;

    /** 답변값 */
    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
}