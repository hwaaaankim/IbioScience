package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryLarge;

import lombok.Data;

@Data
public class CategoryLargeApiDTO {
	private Long id;
	private String name;
	private int mediumCount;  // 추가
	
	public static CategoryLargeApiDTO from(CategoryLarge e, int mediumCount) {
        CategoryLargeApiDTO d = new CategoryLargeApiDTO();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setMediumCount(mediumCount);
        return d;
    }
}