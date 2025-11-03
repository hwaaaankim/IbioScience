package com.dev.IbioScience.enums.product;

public enum DealerGrade {
    A("A등급"),
    B("B등급"),
    C("C등급"),
    D("D등급");

    private final String label;

    DealerGrade(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}