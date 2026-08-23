package com.fafa.domain.service;

import com.fafa.domain.model.reminder.IntervalType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 提醒周期推算领域服务
 *
 * 推算规则（见 phase3-implementation-plan.md 4.4）：
 * - 以被完成期的计划时间（nextRemindTime）为基准推算下期，避免周期漂移
 * - 推算结果已过期（长期过期后补完成）则以当前时间为基准重算一次
 *
 * @author FaFa Team
 * @since 3.0
 */
@Service
public class ReminderScheduleService {

    /**
     * 推算下一期提醒时间
     *
     * @param base  基准时间（被完成期的计划 nextRemindTime）
     * @param type  周期类型
     * @param value 周期值（语义随类型，见 IntervalType）
     * @param now   当前时间（用于过期滚动）
     * @return 下期提醒时间；一次性提醒返回 null
     */
    public LocalDateTime nextAfter(LocalDateTime base, IntervalType type, Integer value, LocalDateTime now) {
        LocalDateTime next = switch (type) {
            case ONCE -> null;
            case DAYS -> base.plusDays(value);
            case WEEKLY -> nextWeekday(base, value);
            case MONTHLY -> sameDayNextMonth(base, value);
            case MONTHS -> base.plusMonths(value);
            case YEARLY -> base.plusYears(1);
        };
        // 过期滚动：推算结果已在过去（长期过期后补完成）→ 以当前时间为基准重算一次
        if (next != null && !next.isAfter(now)) {
            return nextAfter(now, type, value, now.plusNanos(1));
        }
        return next;
    }

    /**
     * 周期配置合法性校验（create/update 复用）
     *
     * @throws IllegalArgumentException 配置非法
     */
    public void validateInterval(IntervalType type, Integer value) {
        switch (type) {
            case ONCE, YEARLY -> {
                // value 忽略（null/任意值均合法）
            }
            case DAYS, MONTHS -> {
                if (value == null || value < 1) {
                    throw new IllegalArgumentException("每N天/每N月的周期值必须 ≥ 1");
                }
            }
            case WEEKLY -> {
                if (value == null || value < 1 || value > 7) {
                    throw new IllegalArgumentException("每周提醒的星期值必须在 1（周一）~ 7（周日）之间");
                }
            }
            case MONTHLY -> {
                if (value == null || value < 1 || value > 31) {
                    throw new IllegalArgumentException("每月提醒的日期值必须在 1 ~ 31 之间");
                }
            }
        }
    }

    /**
     * 下一个目标星期几（同日则顺延一周）
     *
     * @param targetDayOfWeek ISO 星期值 1（周一）~ 7（周日）
     */
    private LocalDateTime nextWeekday(LocalDateTime base, int targetDayOfWeek) {
        int daysAhead = (targetDayOfWeek - base.getDayOfWeek().getValue() + 7) % 7;
        if (daysAhead == 0) {
            daysAhead = 7;
        }
        return base.plusDays(daysAhead);
    }

    /**
     * 下月 value 号（超过下月最大天数时钳制到月末，时刻沿用基准时间）
     */
    private LocalDateTime sameDayNextMonth(LocalDateTime base, int dayOfMonth) {
        YearMonth nextMonth = YearMonth.from(base).plusMonths(1);
        int day = Math.min(dayOfMonth, nextMonth.lengthOfMonth());
        return base.withYear(nextMonth.getYear())
                .withMonth(nextMonth.getMonthValue())
                .withDayOfMonth(day);
    }
}
