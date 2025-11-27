package com.dev.IbioScience.dto.page.productList;

import java.util.List;

import lombok.Data;

@Data
public class ProductOptionGroupDto {
    private Long id;
    private String name;                 // 옵션 그룹명
    private Integer sortOrder;
    private List<ProductOptionDto> options;
}