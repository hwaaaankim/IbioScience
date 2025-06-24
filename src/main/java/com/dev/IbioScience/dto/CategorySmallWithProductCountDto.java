package com.dev.IbioScience.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorySmallWithProductCountDto {
    private Long id;
    private String name;
    private long productCount;
}