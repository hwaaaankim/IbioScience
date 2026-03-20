package com.dev.IbioScience.dto.admin.benefit;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BenefitPageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private int numberOfElements;
    private boolean first;
    private boolean last;

    public static <T> BenefitPageResponse<T> from(Page<T> page) {
        return BenefitPageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}