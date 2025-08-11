package com.dev.IbioScience.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalSmallListDTO {
    private Long id;
    private String name;
    private Long productCount; // 소속 제품 수
}