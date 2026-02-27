package com.dev.IbioScience.dto.customer.auth.crm;

import java.time.LocalDateTime;
import java.util.List;

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
public class MemoPageResponseDto {

	private int page; // 0-based
	private int size; // fixed 10
	private long totalElements;
	private int totalPages;

	private List<Item> content;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Item {
		private Long id;
		private String content;
		private String writerName;
		private LocalDateTime createdAt;
	}
}