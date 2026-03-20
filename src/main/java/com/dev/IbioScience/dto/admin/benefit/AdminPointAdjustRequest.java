package com.dev.IbioScience.dto.admin.benefit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPointAdjustRequest {

    @NotNull
    @Min(1)
    private Long amount;
}