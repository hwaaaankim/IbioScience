package com.dev.IbioScience.dto.page.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandSimpleDTO {
	private Long id;
	private String name;
	private String imageUrl; // 노출용 URL(없으면 null)
}