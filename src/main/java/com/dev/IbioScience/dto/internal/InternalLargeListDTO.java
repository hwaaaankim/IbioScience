package com.dev.IbioScience.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalLargeListDTO {
    private Long id;
    private String name;
    private Long mediumCount; // 소속 중분류 수
}