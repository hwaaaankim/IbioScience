package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.InternalCategoryLarge;
import com.dev.IbioScience.model.product.InternalCategoryMedium;

public interface InternalCategoryMediumRepository extends JpaRepository<InternalCategoryMedium, Long> {
    // 대분류, 중분류명으로 중복 체크
    boolean existsByLargeAndName(InternalCategoryLarge large, String name);

    // 대분류 기준, id 오름차순 전체 조회
    List<InternalCategoryMedium> findAllByLargeOrderByIdAsc(InternalCategoryLarge large);
}