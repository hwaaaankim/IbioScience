package com.dev.IbioScience.dto.order;

import com.dev.IbioScience.enums.order.WishToggleAction;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WishToggleResponse {
    private long count;                 // 토글 후 총 찜 개수
    private WishToggleAction action;    // 이번 요청이 추가인지/삭제인지
}