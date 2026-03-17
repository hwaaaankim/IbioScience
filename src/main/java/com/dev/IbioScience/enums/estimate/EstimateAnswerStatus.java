package com.dev.IbioScience.enums.estimate;

public enum EstimateAnswerStatus {

    WAITING("답변대기"),
    ANSWERED("답변완료");

    private final String label;

    EstimateAnswerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}