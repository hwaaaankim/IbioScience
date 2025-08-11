package com.dev.IbioScience.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalMediumListDTO {
    private Long id;
    private String name;
    private Long smallCount; // 소속 소분류 수
}