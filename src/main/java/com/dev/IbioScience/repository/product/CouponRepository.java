package com.dev.IbioScience.repository.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.CouponListRowDTO;
import com.dev.IbioScience.enums.product.CouponPolicy;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.product.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCouponCode(String couponCode);
    @Query("""
        SELECT c FROM Coupon c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:name IS NULL OR c.couponName LIKE %:name%)
          AND (
                (:startDate IS NULL AND :endDate IS NULL)
                OR (:startDate IS NOT NULL AND :endDate IS NULL AND c.startDate >= :startDate)
                OR (:startDate IS NULL AND :endDate IS NOT NULL AND c.endDate <= :endDate)
                OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND c.startDate >= :startDate AND c.endDate <= :endDate)
              )
        ORDER BY c.id DESC
    """)
    List<Coupon> searchCoupons(
        @Param("status") CouponStatus status,
        @Param("name") String name,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query(value = """
	    SELECT new com.dev.IbioScience.dto.CouponListRowDTO(
	        c.id, c.couponName, c.startDate, c.endDate, c.couponPolicy, c.status,
	        COUNT(p.id)
	    )
	    FROM Coupon c
	    LEFT JOIN c.promotions p
	    WHERE (:name IS NULL OR c.couponName LIKE %:name%)
	      AND (:policy IS NULL OR c.couponPolicy = :policy)
	      AND (:status IS NULL OR c.status = :status)
	      AND (
	            (:startDate IS NULL AND :endDate IS NULL)
	         OR (:startDate IS NOT NULL AND :endDate IS NULL AND c.startDate >= :startDate)
	         OR (:startDate IS NULL AND :endDate IS NOT NULL AND c.endDate <= :endDate)
	         OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND c.startDate >= :startDate AND c.endDate <= :endDate)
	      )
	    GROUP BY c.id, c.couponName, c.startDate, c.endDate, c.couponPolicy, c.status
	    """,
	    countQuery = """
	    SELECT COUNT(c)
	    FROM Coupon c
	    WHERE (:name IS NULL OR c.couponName LIKE %:name%)
	      AND (:policy IS NULL OR c.couponPolicy = :policy)
	      AND (:status IS NULL OR c.status = :status)
	      AND (
	            (:startDate IS NULL AND :endDate IS NULL)
	         OR (:startDate IS NOT NULL AND :endDate IS NULL AND c.startDate >= :startDate)
	         OR (:startDate IS NULL AND :endDate IS NOT NULL AND c.endDate <= :endDate)
	         OR (:startDate IS NOT NULL AND :endDate IS NOT NULL AND c.startDate >= :startDate AND c.endDate <= :endDate)
	      )
	    """
	)
	Page<CouponListRowDTO> searchCouponsPage(
	    @Param("name") String name,
	    @Param("policy") CouponPolicy policy,
	    @Param("status") CouponStatus status,
	    @Param("startDate") LocalDate startDate,
	    @Param("endDate") LocalDate endDate,
	    Pageable pageable
	);

}