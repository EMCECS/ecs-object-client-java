/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

public class ChecksumError extends RuntimeException {
    private static final String MESSAGE = "(expected: %s, actual: %s)";

    private String expectedValue;
    private String actualValue;

    public ChecksumError(String message, String expectedValue, String actualValue) {
        super(message + String.format(MESSAGE, expectedValue, actualValue));
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    public ChecksumError(String message, String expectedValue, String actualValue, Throwable cause) {
        super(message + String.format(MESSAGE, expectedValue, actualValue), cause);
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }
}
