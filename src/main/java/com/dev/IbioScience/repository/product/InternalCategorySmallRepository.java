package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;

public interface InternalCategorySmallRepository extends JpaRepository<InternalCategorySmall, Long> {
    // 중분류, 소분류명으로 중복 체크
    boolean existsByMediumAndName(InternalCategoryMedium medium, String name);

    // 중분류 기준, id 오름차순 전체 조회
    List<InternalCategorySmall> findAllByMediumOrderByIdAsc(InternalCategoryMedium medium);
}