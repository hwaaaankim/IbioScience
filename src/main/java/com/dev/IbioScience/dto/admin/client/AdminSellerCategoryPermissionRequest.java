package com.dev.IbioScience.dto.admin.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminSellerCategoryPermissionRequest {
	private Long largeId;
	private Long mediumId;
	private Long smallId;
}