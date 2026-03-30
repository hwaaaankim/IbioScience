package com.dev.IbioScience.dto.seller.order;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerOrderBatchStatusUpdateRequest {

    @Valid
    @NotEmpty
    private List<SellerOrderStatusChangeItemRequest> items = new ArrayList<>();
}