package com.dev.IbioScience.dto.bankda;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class PaymentCheckDTO {

	private List<Map<String, Object>> requests;
}
