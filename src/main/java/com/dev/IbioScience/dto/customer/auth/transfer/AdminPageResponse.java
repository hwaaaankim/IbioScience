package com.dev.IbioScience.dto.customer.auth.transfer;

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
public class AdminPageResponse<T> {

	private List<T> content;

	private int page;          // 0-base
	private int size;

	private long totalElements;
	private int totalPages;

	private boolean first;
	private boolean last;
}