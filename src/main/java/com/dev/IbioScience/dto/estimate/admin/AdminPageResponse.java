package com.dev.IbioScience.dto.estimate.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> AdminPageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);

        return new AdminPageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page <= 1,
                totalPages == 0 || page >= totalPages
        );
    }
}