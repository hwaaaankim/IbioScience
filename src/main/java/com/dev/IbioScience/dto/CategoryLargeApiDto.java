package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryLarge;

import lombok.Data;

@Data
public class CategoryLargeApiDto {
	private Long id;
	private String name;
	private int mediumCount;  // 추가
	
	public static CategoryLargeApiDto from(CategoryLarge e, int mediumCount) {
        CategoryLargeApiDto d = new CategoryLargeApiDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setMediumCount(mediumCount);
        return d;
    }
}