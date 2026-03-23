package com.dev.IbioScience.model.product.dealer;

import com.dev.IbioScience.model.product.util.Keyword;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "tb_dealer_product_keyword",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_dealer_product_keyword",
            columnNames = {"dealer_product_id", "keyword_id"}
        )
    },
    indexes = {
        @Index(name = "ix_dealer_product_keyword_product", columnList = "dealer_product_id"),
        @Index(name = "ix_dealer_product_keyword_keyword", columnList = "keyword_id")
    }
)
public class DealerProductKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 키워드 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;
}