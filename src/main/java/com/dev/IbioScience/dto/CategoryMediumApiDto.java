package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryMedium;

import lombok.Data;

@Data
public class CategoryMediumApiDto {
    private Long id;
    private String name;
    private Long largeId;
    private int smallCount; // 추가

    public static CategoryMediumApiDto from(CategoryMedium e, int smallCount) {
        CategoryMediumApiDto d = new CategoryMediumApiDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setLargeId(e.getLarge().getId());
        d.setSmallCount(smallCount);
        return d;
    }
}
