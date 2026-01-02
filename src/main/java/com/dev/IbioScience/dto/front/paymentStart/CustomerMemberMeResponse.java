package com.dev.IbioScience.dto.front.paymentStart;

import com.dev.IbioScience.model.auth.embedded.Address;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerMemberMeResponse {
	private Long id;
	private String name;
	private String mobile;
	private String tel;
	private Long point;
	private Address address;
}