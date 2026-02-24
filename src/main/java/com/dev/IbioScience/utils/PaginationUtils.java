package com.dev.IbioScience.utils;

import org.springframework.data.domain.Page;

public class PaginationUtils {

    public static java.util.List<Integer> makePageNumbers(Page<?> page) {
        if (page == null || page.getTotalPages() <= 0) return java.util.List.of(0);

        int totalPages = page.getTotalPages();
        int current = page.getNumber();

        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, start + 4);

        // end가 줄어든 경우 start를 다시 보정해서 최대 5개 유지
        start = Math.max(0, end - 4);

        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (int i = start; i <= end; i++) list.add(i);
        return list;
    }
}