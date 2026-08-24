package com.fafa.domain.model.report;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record ReportPeriod(ReportPeriodType type, LocalDate startDate, LocalDate endDate) {

    public static ReportPeriod resolve(ReportPeriodType type, LocalDate anchorDate) {
        LocalDate anchor = anchorDate == null ? LocalDate.now() : anchorDate;
        return switch (type) {
            case WEEK -> new ReportPeriod(type,
                    anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case MONTH -> new ReportPeriod(type, anchor.withDayOfMonth(1),
                    anchor.withDayOfMonth(anchor.lengthOfMonth()));
            case QUARTER -> {
                int firstMonth = ((anchor.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(anchor.getYear(), firstMonth, 1);
                yield new ReportPeriod(type, start, start.plusMonths(2)
                        .withDayOfMonth(start.plusMonths(2).lengthOfMonth()));
            }
            case YEAR -> new ReportPeriod(type, LocalDate.of(anchor.getYear(), 1, 1),
                    LocalDate.of(anchor.getYear(), 12, 31));
        };
    }

    public String key() {
        return type.getCode() + ":" + startDate + ":" + endDate;
    }
}
