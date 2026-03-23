package com.dev.IbioScience.model.product.dealer;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_product_option_group",
    indexes = {
        @Index(name = "ix_dealer_option_group_product", columnList = "dealer_product_id"),
        @Index(name = "ix_dealer_option_group_sort", columnList = "sort_order")
    }
)
public class DealerProductOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 옵션 그룹명 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 정렬순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 옵션 리스트 */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductOption> options = new ArrayList<>();
}