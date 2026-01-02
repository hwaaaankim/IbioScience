package com.dev.IbioScience.dto.order;

import java.util.List;

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
public class OrderDetailViewDTO {

    private String orderNo;
    private String status;

    private String receiverName;
    private String fullAddress;

    private Long sumPrice;
    private Long shippingFee;
    private Long baseDiscount;
    private Long couponDiscount;
    private Long pointUsed;
    private Long grandTotal;
    private Long expectPoint;

    private String paymentMethod; // 표시용
    private String bankInfoText;  // 계좌이체 안내문구용(테스트)

    private List<OrderDetailItemViewDTO> items;
}