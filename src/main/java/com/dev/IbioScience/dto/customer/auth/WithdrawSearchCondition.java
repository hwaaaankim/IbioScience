package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawSearchCondition {

	private Integer page; // 0-base
	private Integer size; // 10/30/50/100

	private LocalDate fromDate; // nullable
	private LocalDate toDate;   // nullable

	private SearchField searchField; // USERNAME/CONTACT/NAME
	private String keyword;          // nullable

	private ApplyType applyType; // ALL/PERSONAL/BUSINESS

	public enum SearchField {
		USERNAME, CONTACT, NAME
	}

	public enum ApplyType {
		ALL, PERSONAL, BUSINESS
	}
}