package com.lody.virtual.custom.models;

public class ProtectorInfo {
    private String string;
    private String expectedValue;

    public ProtectorInfo(String string, String expectedValue) {
        this.string = string;
        this.expectedValue = expectedValue;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }
}
