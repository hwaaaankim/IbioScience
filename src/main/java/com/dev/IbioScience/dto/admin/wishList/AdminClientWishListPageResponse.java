package com.dev.IbioScience.dto.admin.wishList;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientWishListPageResponse {

    private List<AdminClientWishListRowDTO> content;

    private int page;
    private int size;

    private long totalElements;
    private int totalPages;
    private int numberOfElements;

    private boolean first;
    private boolean last;
}