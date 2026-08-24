package com.fafa.domain.service.expense;

import com.fafa.domain.model.expense.ExpenseCategory;
import com.fafa.domain.model.expense.ExpenseRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 费用统计纯领域服务，不读数据库、不读取当前用户上下文。 */
public final class ExpenseStatisticsCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    public Result calculate(List<ExpenseRecord> records, LocalDate startDate, LocalDate endDate,
                            String period) {
        if (records == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("统计参数无效");
        }
        String normalizedPeriod = period == null ? "month" : period.trim().toLowerCase();
        if (!normalizedPeriod.equals("month") && !normalizedPeriod.equals("year")) {
            throw new IllegalArgumentException("统计周期仅支持 month 或 year");
        }
        Map<ExpenseCategory, BigDecimal> categoryTotals = new EnumMap<>(ExpenseCategory.class);
        List<ExpenseRecord> validRecords = records.stream()
                .filter(record -> !record.isDeleted())
                .filter(record -> !record.getExpenseDate().isBefore(startDate)
                        && !record.getExpenseDate().isAfter(endDate))
                .toList();
        BigDecimal total = validRecords.stream().map(record -> record.getMoney().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        for (ExpenseRecord record : validRecords) {
            categoryTotals.merge(record.getCategory(), record.getMoney().amount(), BigDecimal::add);
        }
        List<CategoryTotal> categories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(ExpenseCategory::getSortOrder)))
                .map(entry -> new CategoryTotal(entry.getKey(), scale(entry.getValue()), percentage(entry.getValue(), total)))
                .toList();
        categories = adjustLastPercentage(categories);
        List<TrendPoint> trend = normalizedPeriod.equals("month")
                ? dailyTrend(validRecords, startDate, endDate)
                : monthlyTrend(validRecords, startDate, endDate);
        return new Result(scale(total), validRecords.size(), categories, trend, normalizedPeriod);
    }

    private List<TrendPoint> dailyTrend(List<ExpenseRecord> records, LocalDate start, LocalDate end) {
        List<TrendPoint> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate current = date;
            result.add(new TrendPoint(current.toString(), scale(records.stream()
                    .filter(record -> record.getExpenseDate().equals(current))
                    .map(record -> record.getMoney().amount()).reduce(BigDecimal.ZERO, BigDecimal::add)),
                    (int) records.stream().filter(record -> record.getExpenseDate().equals(current)).count()));
        }
        return result;
    }

    private List<TrendPoint> monthlyTrend(List<ExpenseRecord> records, LocalDate start, LocalDate end) {
        List<TrendPoint> result = new ArrayList<>();
        YearMonth first = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            YearMonth current = month;
            result.add(new TrendPoint(current.toString(), scale(records.stream()
                    .filter(record -> YearMonth.from(record.getExpenseDate()).equals(current))
                    .map(record -> record.getMoney().amount()).reduce(BigDecimal.ZERO, BigDecimal::add)),
                    (int) records.stream().filter(record -> YearMonth.from(record.getExpenseDate()).equals(current)).count()));
        }
        return result;
    }

    private List<CategoryTotal> adjustLastPercentage(List<CategoryTotal> categories) {
        if (categories.isEmpty()) {
            return categories;
        }
        BigDecimal sum = categories.stream().map(CategoryTotal::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal residual = HUNDRED.subtract(sum).setScale(2, RoundingMode.HALF_UP);
        if (residual.signum() == 0) {
            return categories;
        }
        int last = categories.size() - 1;
        List<CategoryTotal> adjusted = new ArrayList<>(categories);
        CategoryTotal item = adjusted.get(last);
        adjusted.set(last, new CategoryTotal(item.category(), item.amount(),
                item.percentage().add(residual).setScale(2, RoundingMode.HALF_UP)));
        return List.copyOf(adjusted);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return ZERO;
        }
        return amount.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Result(BigDecimal totalAmount, int expenseCount, List<CategoryTotal> categories,
                         List<TrendPoint> trend, String period) {
    }

    public record CategoryTotal(ExpenseCategory category, BigDecimal amount, BigDecimal percentage) {
    }

    public record TrendPoint(String period, BigDecimal amount, int expenseCount) {
    }
}
