package com.fafa.domain.model.report;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPeriodTest {
    @Test
    void month_shouldResolveToCalendarMonth() {
        ReportPeriod period = ReportPeriod.resolve(ReportPeriodType.MONTH, LocalDate.of(2026, 8, 23));

        assertThat(period.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(period.endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(period.key()).isEqualTo("month:2026-08-01:2026-08-31");
    }

    @Test
    void week_shouldUseMondayToSunday() {
        ReportPeriod period = ReportPeriod.resolve(ReportPeriodType.WEEK, LocalDate.of(2026, 8, 23));

        assertThat(period.startDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(period.endDate()).isEqualTo(LocalDate.of(2026, 8, 23));
    }
}
