package com.dev.IbioScience.dto.customer.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class ClientApplyPaginationDto {

    private final int currentPage;   // 0-based
    private final int totalPages;    // 최소 1
    private final int firstPage;     // 0
    private final int lastPage;      // totalPages-1

    private final int prevPage;
    private final int nextPage;

    private final boolean isFirst;
    private final boolean isLast;

    private final List<Integer> pageNumbers; // 0-based, 최대 5개

    private ClientApplyPaginationDto(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        this.totalPages = Math.max(totalPages, 1);
        this.firstPage = 0;
        this.lastPage = this.totalPages - 1;

        this.prevPage = Math.max(this.currentPage - 1, 0);
        this.nextPage = Math.min(this.currentPage + 1, this.lastPage);

        this.isFirst = (this.currentPage <= 0);
        this.isLast = (this.currentPage >= this.lastPage);

        int groupStart = (this.currentPage / 5) * 5;
        int groupEnd = Math.min(groupStart + 4, this.lastPage);

        this.pageNumbers = new ArrayList<>();
        for (int i = groupStart; i <= groupEnd; i++) {
            this.pageNumbers.add(i);
        }
    }

    public static ClientApplyPaginationDto of(Page<?> page) {
        int current = page == null ? 0 : page.getNumber();
        int total = page == null ? 1 : page.getTotalPages();
        return new ClientApplyPaginationDto(current, total);
    }
}