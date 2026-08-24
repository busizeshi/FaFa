package com.fafa.domain.service;

import com.fafa.domain.model.expense.ExpenseCategory;
import com.fafa.domain.model.expense.ExpenseRecord;
import com.fafa.domain.model.expense.Money;
import com.fafa.domain.service.expense.ExpenseStatisticsCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseStatisticsCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 3);

    @Test
    void monthStatistics_shouldFillZeroTrendAndCorrectPercentages() {
        List<ExpenseRecord> records = List.of(
                ExpenseRecord.createManual(3L, 1L, ExpenseCategory.FOOD,
                        new Money(new BigDecimal("10.00"), "CNY"), START, "食品", null,
                        null, null, null, END),
                ExpenseRecord.createManual(3L, 1L, ExpenseCategory.MEDICAL,
                        new Money(new BigDecimal("20.00"), "CNY"), END, "医疗", null,
                        null, null, null, END));

        ExpenseStatisticsCalculator.Result result = new ExpenseStatisticsCalculator()
                .calculate(records, START, END, "month");

        assertThat(result.totalAmount()).isEqualByComparingTo("30.00");
        assertThat(result.expenseCount()).isEqualTo(2);
        assertThat(result.categories()).extracting(ExpenseStatisticsCalculator.CategoryTotal::percentage)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("66.67"));
        assertThat(result.trend()).extracting(ExpenseStatisticsCalculator.TrendPoint::amount)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("0.00"), new BigDecimal("20.00"));
    }
}
