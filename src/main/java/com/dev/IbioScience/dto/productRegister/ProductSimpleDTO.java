package com.dev.IbioScience.dto.productRegister;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSimpleDTO {
    private Long id;
    private String code;
    private String name;
}