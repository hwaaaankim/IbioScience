package com.dev.IbioScience.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateResponseDTO {
    private Long orderId;
    private String orderNo;
    private String status; // PAYMENT_PENDING
}