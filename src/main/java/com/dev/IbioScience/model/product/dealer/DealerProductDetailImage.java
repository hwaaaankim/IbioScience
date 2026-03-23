package com.dev.IbioScience.model.product.dealer;

import java.time.LocalDateTime;

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
    name = "tb_dealer_product_detail_image",
    indexes = {
        @Index(name = "ix_dealer_product_detail_image_product", columnList = "dealer_product_id"),
        @Index(name = "ix_dealer_product_detail_image_sort", columnList = "sort_order")
    }
)
public class DealerProductDetailImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 공개 URL */
    @Column(name = "url", length = 500)
    private String url;

    /** 저장 PATH */
    @Column(name = "path", length = 500)
    private String path;

    /** 파일명 */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** 원본 파일명 */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** 파일크기(byte) */
    @Column(name = "size")
    private Integer size;

    /** 업로드 일시 */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /** 실제 사용 여부 */
    @Column(name = "in_use")
    private Boolean inUse;

    /** 정렬순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;
}