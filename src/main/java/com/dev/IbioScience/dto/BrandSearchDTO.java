package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.Brand;

import lombok.Data;

@Data
public class BrandSearchDTO {
    private Long id;
    private String name;
    private String imageRoad;
    public static BrandSearchDTO fromEntity(Brand b) {
        BrandSearchDTO dto = new BrandSearchDTO();
        dto.setId(b.getId());
        dto.setName(b.getName());
        dto.setImageRoad(b.getImageRoad());
        return dto;
    }
}