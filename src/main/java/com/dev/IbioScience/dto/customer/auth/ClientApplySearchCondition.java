package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientApplySearchCondition {

    private Integer page;   // 0-based
    private Integer size;   // 10/30/50/100

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private SearchField searchField; // USERNAME / MOBILE / NAME
    private String keyword;

    private ApplyType applyType;     // ALL / PERSONAL / BUSINESS

    public enum SearchField {
        USERNAME, MOBILE, NAME
    }

    public enum ApplyType {
        ALL, PERSONAL, BUSINESS
    }
}