package com.dev.IbioScience.dto.customer.auth.crm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {
    private String postcode;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
}