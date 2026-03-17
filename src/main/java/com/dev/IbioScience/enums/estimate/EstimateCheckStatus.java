package com.dev.IbioScience.enums.estimate;

public enum EstimateCheckStatus {

    UNCHECKED("미확인"),
    CHECKED("확인완료");

    private final String label;

    EstimateCheckStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}