package com.dev.IbioScience.enums.estimate.admin;

import lombok.Getter;

@Getter
public enum AdminEstimateMemberSearchType {

    USER_ID("유저아이디"),
    EMAIL("이메일"),
    CONTACT_PHONE("연락처"),
    NAME("이름");

    private final String label;

    AdminEstimateMemberSearchType(String label) {
        this.label = label;
    }
}