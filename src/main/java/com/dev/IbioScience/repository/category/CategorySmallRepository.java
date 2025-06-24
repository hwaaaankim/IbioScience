package com.dev.IbioScience.repository.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.dto.CategorySmallWithProductCountDto;
import com.dev.IbioScience.model.product.category.CategorySmall;

@Repository
public interface CategorySmallRepository extends JpaRepository<CategorySmall, Long> {
	
	@Query("SELECT new com.dev.IbioScience.dto.CategorySmallWithProductCountDto(s.id, s.name, COUNT(spc)) " +
		       "FROM CategorySmall s " +
		       "LEFT JOIN MediumSmallCategory msc ON msc.small = s " +
		       "LEFT JOIN SmallProductCategory spc ON spc.small = s " +
		       "WHERE msc.medium.id = :mediumId " +
		       "GROUP BY s.id, s.name")
	List<CategorySmallWithProductCountDto> findWithProductCountByMediumId(@Param("mediumId") Long mediumId);

	Optional<CategorySmall> findByName(String name);
    boolean existsByName(String name);
}