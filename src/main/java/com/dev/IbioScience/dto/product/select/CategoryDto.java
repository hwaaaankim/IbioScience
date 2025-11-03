package com.dev.IbioScience.dto.product.select;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryDto {
	@Data
	@Builder
	public static class Large {
		private Long id;
		private String name;
	}

	@Data
	@Builder
	public static class Medium {
		private Long id;
		private String name;
		private Long largeId;
	}

	@Data
	@Builder
	public static class Small {
		private Long id;
		private String name;
	}

	/** 내부 카테고리 DTO */
	@Data
	@Builder
	public static class InternalLarge {
		private Long id;
		private String name;
	}

	@Data
	@Builder
	public static class InternalMedium {
		private Long id;
		private String name;
		private Long largeId;
	}

	@Data
	@Builder
	public static class InternalSmall {
		private Long id;
		private String name;
		private Long mediumId;
	}
}