package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.Product;

/**
 * 인덱스 카드 전용 쿼리 레포지토리
 * - JpaRepository<Product, Long> 상속으로 스프링이 자동 Bean 생성
 * - nativeQuery + List<Object[]> 형태 유지 (서비스에서 DTO 매핑)
 */
@Repository
public interface ProductIndexRepository extends JpaRepository<Product, Long> {

	@Query(value =
        "SELECT " +
        "  p.id                    AS id, " +
        "  p.name                  AS name, " +
        "  p.sale_price            AS salePrice, " +
        "  p.consumer_price        AS consumerPrice, " +
        "  IFNULL(p.sales_count,0) AS salesCount, " +
        "  IFNULL(p.view_count,0)  AS viewCount, " +
        "  ROUND(IFNULL(AVG(r.rating),0),1) AS averageRating, " +
        "  IFNULL(COUNT(r.id),0)            AS reviewCount, " +
        "  (SELECT i.url FROM tb_product_image i " +
        "    WHERE i.product_id=p.id AND i.type='MAIN' " +
        "    ORDER BY IFNULL(i.sort_order,999999) ASC, i.id ASC LIMIT 1) AS mainImageUrl, " +
        "  CAST(MAX(CASE WHEN pr.type='DISCOUNT' THEN pr.discount_percent END) AS SIGNED) AS discountRate, " +
        "  GROUP_CONCAT(DISTINCT CASE WHEN pr.type <> 'DISCOUNT' THEN pr.name END SEPARATOR '||') AS promotionLabels " +
        "FROM tb_product p " +
        "LEFT JOIN tb_product_review r ON r.product_id=p.id " +
        "LEFT JOIN tb_product_promotion_mapping ppm ON ppm.product_id=p.id " +
        "LEFT JOIN tb_promotion pr ON pr.id=ppm.promotion_id " +
        "  AND pr.active=1 " +
        "  AND ( (pr.term='ALWAYS') OR (pr.term='PERIOD' AND pr.start_date<=CURRENT_DATE AND pr.end_date>=CURRENT_DATE) ) " +
        "WHERE p.state='NORMAL' " +
        "GROUP BY p.id " +
        "ORDER BY IFNULL(p.view_count,0) DESC, p.id DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findTopViewedRaw(@Param("limit") int limit);

    @Query(value =
        "SELECT " +
        "  p.id, p.name, p.sale_price, p.consumer_price, IFNULL(p.sales_count,0), IFNULL(p.view_count,0), " +
        "  ROUND(IFNULL(AVG(r.rating),0),1), IFNULL(COUNT(r.id),0), " +
        "  (SELECT i.url FROM tb_product_image i WHERE i.product_id=p.id AND i.type='MAIN' " +
        "     ORDER BY IFNULL(i.sort_order,999999), i.id LIMIT 1), " +
        "  CAST(MAX(CASE WHEN pr.type='DISCOUNT' THEN pr.discount_percent END) AS SIGNED), " +
        "  GROUP_CONCAT(DISTINCT CASE WHEN pr.type <> 'DISCOUNT' THEN pr.name END SEPARATOR '||') " +
        "FROM tb_product p " +
        "LEFT JOIN tb_product_review r ON r.product_id=p.id " +
        "LEFT JOIN tb_product_promotion_mapping ppm ON ppm.product_id=p.id " +
        "LEFT JOIN tb_promotion pr ON pr.id=ppm.promotion_id " +
        "  AND pr.active=1 " +
        "  AND ( (pr.term='ALWAYS') OR (pr.term='PERIOD' AND pr.start_date<=CURRENT_DATE AND pr.end_date>=CURRENT_DATE) ) " +
        "WHERE p.state='NORMAL' " +
        "GROUP BY p.id " +
        "ORDER BY IFNULL(p.sales_count,0) DESC, p.id DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findTopSalesRaw(@Param("limit") int limit);

    @Query(value =
        "SELECT " +
        "  p.id, p.name, p.sale_price, p.consumer_price, IFNULL(p.sales_count,0), IFNULL(p.view_count,0), " +
        "  ROUND(IFNULL(AVG(r.rating),0),1), IFNULL(COUNT(r.id),0), " +
        "  (SELECT i.url FROM tb_product_image i WHERE i.product_id=p.id AND i.type='MAIN' " +
        "     ORDER BY IFNULL(i.sort_order,999999), i.id LIMIT 1), " +
        "  CAST(MAX(CASE WHEN pr.type='DISCOUNT' THEN pr.discount_percent END) AS SIGNED), " +
        "  GROUP_CONCAT(DISTINCT CASE WHEN pr.type <> 'DISCOUNT' THEN pr.name END SEPARATOR '||') " +
        "FROM tb_product p " +
        "JOIN tb_product_promotion_mapping ppm0 ON ppm0.product_id=p.id " +
        "JOIN tb_promotion pr0 ON pr0.id=ppm0.promotion_id " +
        "LEFT JOIN tb_product_review r ON r.product_id=p.id " +
        "LEFT JOIN tb_product_promotion_mapping ppm ON ppm.product_id=p.id " +
        "LEFT JOIN tb_promotion pr ON pr.id=ppm.promotion_id " +
        "  AND pr.active=1 " +
        "  AND ( (pr.term='ALWAYS') OR (pr.term='PERIOD' AND pr.start_date<=CURRENT_DATE AND pr.end_date>=CURRENT_DATE) ) " +
        "WHERE p.state='NORMAL' " +
        "GROUP BY p.id " +
        "ORDER BY COALESCE(p.created_at, '1970-01-01 00:00:00') ASC, p.id ASC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findPromotionOldestRaw(@Param("limit") int limit);

    /**
     * ✅ 이벤트가 없을 때 사용할 랜덤 상품 조회
     * - 기존 raw 컬럼 배열(0~10)과 완전히 동일한 SELECT/조인/그룹 형태 유지
     * - ORDER BY RAND() 로 랜덤 정렬
     *
     * 주의:
     * - 데이터가 매우 많아지면 ORDER BY RAND()는 성능이 떨어질 수 있습니다.
     *   (그 경우 더 가벼운 랜덤 샘플링 방식으로 교체 가능)
     */
    @Query(value =
        "SELECT " +
        "  p.id, p.name, p.sale_price, p.consumer_price, IFNULL(p.sales_count,0), IFNULL(p.view_count,0), " +
        "  ROUND(IFNULL(AVG(r.rating),0),1), IFNULL(COUNT(r.id),0), " +
        "  (SELECT i.url FROM tb_product_image i WHERE i.product_id=p.id AND i.type='MAIN' " +
        "     ORDER BY IFNULL(i.sort_order,999999), i.id LIMIT 1), " +
        "  CAST(MAX(CASE WHEN pr.type='DISCOUNT' THEN pr.discount_percent END) AS SIGNED), " +
        "  GROUP_CONCAT(DISTINCT CASE WHEN pr.type <> 'DISCOUNT' THEN pr.name END SEPARATOR '||') " +
        "FROM tb_product p " +
        "LEFT JOIN tb_product_review r ON r.product_id=p.id " +
        "LEFT JOIN tb_product_promotion_mapping ppm ON ppm.product_id=p.id " +
        "LEFT JOIN tb_promotion pr ON pr.id=ppm.promotion_id " +
        "  AND pr.active=1 " +
        "  AND ( (pr.term='ALWAYS') OR (pr.term='PERIOD' AND pr.start_date<=CURRENT_DATE AND pr.end_date>=CURRENT_DATE) ) " +
        "WHERE p.state='NORMAL' " +
        "GROUP BY p.id " +
        "ORDER BY RAND() " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findRandomRaw(@Param("limit") int limit);
}