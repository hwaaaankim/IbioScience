package com.dev.IbioScience.enums.estimate.admin;

import lombok.Getter;

@Getter
public enum AdminEstimateDateType {

    REQUESTED_AT("신청일기준"),
    CHECKED_AT("확인일기준"),
    ANSWERED_AT("답변일기준");

    private final String label;

    AdminEstimateDateType(String label) {
        this.label = label;
    }
}