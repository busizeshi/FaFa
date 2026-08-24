package com.fafa.application.service;

import com.fafa.application.dto.expense.ExpenseStatisticsDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.expense.ExpenseRecord;
import com.fafa.domain.repository.ExpenseRecordRepository;
import com.fafa.domain.service.expense.ExpenseStatisticsCalculator;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 开支统计应用服务，负责周期边界、归属校验和结果 DTO 转换。 */
@Service
@RequiredArgsConstructor
public class ExpenseStatisticsApplicationService {

    private final ExpenseRecordRepository repository;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;
    private final ExpenseStatisticsCalculator calculator = new ExpenseStatisticsCalculator();

    public ExpenseStatisticsDTO statistics(Long petId, String period, LocalDate anchorDate) {
        return statisticsForUser(currentUserId(), petId, period, anchorDate);
    }

    public ExpenseStatisticsDTO statisticsForUser(Long userId, Long petId, String period,
                                                  LocalDate anchorDate) {
        ensurePet(userId, petId);
        PeriodRange range = periodRange(period, anchorDate);
        List<ExpenseRecord> records = repository.findActiveByUserIdAndPetIdAndDateRange(userId, petId,
                range.start(), range.end());
        ExpenseStatisticsCalculator.Result result;
        try {
            result = calculator.calculate(records, range.start(), range.end(), range.period());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_RANGE_INVALID, ex.getMessage());
        }
        List<ExpenseStatisticsDTO.CategoryBreakdown> categories = result.categories().stream()
                .map(item -> new ExpenseStatisticsDTO.CategoryBreakdown(item.category().getCode(),
                        item.category().getLabel(), item.amount(), item.percentage()))
                .toList();
        List<ExpenseStatisticsDTO.TrendPoint> trend = result.trend().stream()
                .map(item -> new ExpenseStatisticsDTO.TrendPoint(item.period(), item.amount(), item.expenseCount()))
                .toList();
        String dataStatus = range.complete() ? "complete" : "partial";
        List<String> limitations = range.complete()
                ? List.of("仅统计未删除的开支记录")
                : List.of("当前统计周期尚未结束，未来日期不纳入统计");
        return new ExpenseStatisticsDTO(petId, range.period(), range.start(), range.end(),
                properties.getExpense().getDefaultCurrency(), result.totalAmount(), result.expenseCount(),
                dataStatus, properties.getExpense().getCalculationVersion(), categories, trend, limitations);
    }

    private PeriodRange periodRange(String period, LocalDate anchorDate) {
        if (anchorDate == null || anchorDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.EXPENSE_RANGE_INVALID, "统计锚点不能晚于今天");
        }
        String normalized = period == null ? "month" : period.trim().toLowerCase();
        LocalDate start;
        LocalDate fullEnd;
        if (normalized.equals("month")) {
            YearMonth month = YearMonth.from(anchorDate);
            start = month.atDay(1);
            fullEnd = month.atEndOfMonth();
        } else if (normalized.equals("year")) {
            start = LocalDate.of(anchorDate.getYear(), 1, 1);
            fullEnd = LocalDate.of(anchorDate.getYear(), 12, 31);
        } else {
            throw new BusinessException(ErrorCode.EXPENSE_RANGE_INVALID, "统计周期仅支持 month 或 year");
        }
        LocalDate end = fullEnd.isAfter(anchorDate) ? anchorDate : fullEnd;
        if (ChronoUnit.DAYS.between(start, end) > properties.getExpense().getMaxRangeDays()) {
            throw new BusinessException(ErrorCode.EXPENSE_RANGE_INVALID);
        }
        return new PeriodRange(normalized, start, end, end.equals(fullEnd));
    }

    private void ensurePet(Long userId, Long petId) {
        try {
            petApplicationService.checkPetOwnership(userId, petId);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND, "宠物不存在或无权访问");
        }
    }

    private Long currentUserId() {
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    }

    private record PeriodRange(String period, LocalDate start, LocalDate end, boolean complete) {
    }
}
