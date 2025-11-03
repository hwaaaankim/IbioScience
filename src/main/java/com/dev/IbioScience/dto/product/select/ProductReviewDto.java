package com.dev.IbioScience.dto.product.select;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductReviewDto {
	private Long id;
	private Long memberId;
	private String memberDisplayName;
	private Integer rating;
	private String content;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<ReviewImageDto> images;

	@Data
	@Builder
	public static class ReviewImageDto {
		private String url;
		private String path;
		private String fileName;
		private Integer sortOrder;
	}
}