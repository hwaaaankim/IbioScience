package com.dev.IbioScience.repository.product.register;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.product.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	@Query("select i from ProductImage i where i.product.id = :pid order by i.type asc, i.sortOrder asc, i.id asc")
	List<ProductImage> findAllByProductOrder(@Param("pid") Long productId);

	@Query("""
			    select pi
			    from ProductImage pi
			    where pi.product.id in :productIds
			      and pi.type = com.dev.IbioScience.enums.product.ProductImageType.MAIN
			    order by pi.product.id asc, pi.sortOrder asc nulls last, pi.id asc
			""")
	List<ProductImage> findMainImagesByProductIds(@Param("productIds") List<Long> productIds);

	List<ProductImage> findByProduct_IdInAndTypeOrderByProduct_IdAscSortOrderAscIdAsc(Collection<Long> productIds,
			ProductImageType type);

}
