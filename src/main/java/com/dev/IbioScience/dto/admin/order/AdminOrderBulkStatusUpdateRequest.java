package com.dev.IbioScience.dto.admin.order;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.order.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderBulkStatusUpdateRequest {

    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long orderId;
        private OrderStatus status;
    }
}