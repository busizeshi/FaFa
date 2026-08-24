package com.fafa.domain.model.reminder;

public enum DeliveryStatus {
    PENDING("pending"), SENT("sent"), FAILED("failed"), SKIPPED("skipped");

    private final String code;
    DeliveryStatus(String code) { this.code = code; }
    public String getCode() { return code; }
}
