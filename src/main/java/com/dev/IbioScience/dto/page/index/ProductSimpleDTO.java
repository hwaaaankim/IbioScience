package com.dev.IbioScience.dto.page.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 메가메뉴 리스트에 찍히는 최소 제품 정보 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDTO {
	private Long id;
	private String name;
	private Long brandId; // 브랜딩 필터에 사용
}