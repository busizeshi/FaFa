package com.fafa.domain.model.report;

public enum ReportPeriodType {
    WEEK, MONTH, QUARTER, YEAR;

    public static ReportPeriodType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("报告周期不能为空");
        }
        return switch (code.trim().toLowerCase()) {
            case "week" -> WEEK;
            case "month" -> MONTH;
            case "quarter" -> QUARTER;
            case "year" -> YEAR;
            default -> throw new IllegalArgumentException("报告周期仅支持 week/month/quarter/year");
        };
    }

    public String getCode() {
        return name().toLowerCase();
    }
}
