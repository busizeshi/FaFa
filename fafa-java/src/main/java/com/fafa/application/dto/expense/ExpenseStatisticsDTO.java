package com.fafa.application.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 开支统计响应，趋势点由后端补齐零值，便于前端直接绘图。 */
public record ExpenseStatisticsDTO(Long petId, String period, LocalDate startDate, LocalDate endDate,
                                   String currency, BigDecimal totalAmount, int expenseCount,
                                   String dataStatus, String calculationVersion,
                                   List<CategoryBreakdown> categoryBreakdown,
                                   List<TrendPoint> trend, List<String> limitations) {

    public record CategoryBreakdown(String category, String categoryLabel, BigDecimal amount,
                                    BigDecimal percentage) {
    }

    public record TrendPoint(String period, BigDecimal amount, int expenseCount) {
    }
}
