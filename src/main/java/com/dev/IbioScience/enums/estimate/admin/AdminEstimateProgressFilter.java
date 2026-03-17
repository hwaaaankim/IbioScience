package com.dev.IbioScience.enums.estimate.admin;

import lombok.Getter;

@Getter
public enum AdminEstimateProgressFilter {

    UNCHECKED("미확인"),
    CHECKED("확인완료"),
    ANSWERED("답변완료");

    private final String label;

    AdminEstimateProgressFilter(String label) {
        this.label = label;
    }
}