package com.fafa.domain.service;

import com.fafa.domain.model.reminder.IntervalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 提醒周期推算领域服务单元测试
 *
 * @author FaFa Team
 * @since 3.0
 */
class ReminderScheduleServiceTest {

    private final ReminderScheduleService service = new ReminderScheduleService();

    // ---------- nextAfter：六种周期基准推算 ----------

    @Test
    @DisplayName("nextAfter：一次性提醒返回 null")
    void nextAfter_once_shouldReturnNull() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        assertThat(service.nextAfter(base, IntervalType.ONCE, null, base.plusHours(1))).isNull();
    }

    @Test
    @DisplayName("nextAfter：每N天以基准时间加N天")
    void nextAfter_days_shouldAddDays() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime now = base.plusDays(1);

        LocalDateTime next = service.nextAfter(base, IntervalType.DAYS, 3, now);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 9, 4, 9, 0));
    }

    @Test
    @DisplayName("nextAfter：每周取下一个目标星期几")
    void nextAfter_weekly_shouldPickNextWeekday() {
        // 基准 2026-09-02 是周三
        LocalDateTime base = LocalDateTime.of(2026, 9, 2, 9, 0);
        LocalDateTime now = base.plusDays(1);

        // 目标周五（5）→ +2 天
        assertThat(service.nextAfter(base, IntervalType.WEEKLY, 5, now))
                .isEqualTo(LocalDateTime.of(2026, 9, 4, 9, 0));
        // 目标周一（1）→ +5 天
        assertThat(service.nextAfter(base, IntervalType.WEEKLY, 1, now))
                .isEqualTo(LocalDateTime.of(2026, 9, 7, 9, 0));
    }

    @Test
    @DisplayName("nextAfter：每周同日顺延一周（不取当天）")
    void nextAfter_weekly_sameDay_shouldAddSevenDays() {
        // 基准 2026-09-06 是周日，目标周日（7）→ 下周日
        LocalDateTime base = LocalDateTime.of(2026, 9, 6, 9, 0);
        LocalDateTime now = base.plusDays(1);

        assertThat(service.nextAfter(base, IntervalType.WEEKLY, 7, now))
                .isEqualTo(LocalDateTime.of(2026, 9, 13, 9, 0));
    }

    @Test
    @DisplayName("nextAfter：每月取下月目标日（时刻沿用基准）")
    void nextAfter_monthly_shouldPickSameDayNextMonth() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 15, 20, 30);
        LocalDateTime now = base.plusDays(1);

        assertThat(service.nextAfter(base, IntervalType.MONTHLY, 1, now))
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 20, 30));
    }

    @Test
    @DisplayName("nextAfter：每月目标日超过下月天数时钳制到月末")
    void nextAfter_monthly_shouldClampToMonthEnd() {
        // 2026-01-31，目标 31 号 → 2 月只有 28 天（2026 非闰年）→ 2/28
        LocalDateTime base = LocalDateTime.of(2026, 1, 31, 9, 0);
        LocalDateTime now = base.plusDays(1);

        assertThat(service.nextAfter(base, IntervalType.MONTHLY, 31, now))
                .isEqualTo(LocalDateTime.of(2026, 2, 28, 9, 0));

        // 闰年：2028-01-31 → 2028-02-29
        LocalDateTime leapBase = LocalDateTime.of(2028, 1, 31, 9, 0);
        assertThat(service.nextAfter(leapBase, IntervalType.MONTHLY, 31, leapBase.plusDays(1)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 0));

        // 30 天月份：2026-03-31，目标 31 号 → 4 月 30 天 → 4/30
        LocalDateTime aprilBase = LocalDateTime.of(2026, 3, 31, 9, 0);
        assertThat(service.nextAfter(aprilBase, IntervalType.MONTHLY, 31, aprilBase.plusDays(1)))
                .isEqualTo(LocalDateTime.of(2026, 4, 30, 9, 0));
    }

    @Test
    @DisplayName("nextAfter：每N个月以基准时间加N月")
    void nextAfter_months_shouldAddMonths() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime now = base.plusDays(1);

        assertThat(service.nextAfter(base, IntervalType.MONTHS, 3, now))
                .isEqualTo(LocalDateTime.of(2026, 12, 1, 9, 0));
    }

    @Test
    @DisplayName("nextAfter：每年以基准时间加1年（闰年 2/29 由 plusYears 钳制）")
    void nextAfter_yearly_shouldAddOneYear() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime now = base.plusDays(1);

        assertThat(service.nextAfter(base, IntervalType.YEARLY, null, now))
                .isEqualTo(LocalDateTime.of(2027, 9, 1, 9, 0));

        // 闰年生日：2028-02-29 → 2029-02-28
        LocalDateTime leapBirthday = LocalDateTime.of(2028, 2, 29, 9, 0);
        assertThat(service.nextAfter(leapBirthday, IntervalType.YEARLY, null, leapBirthday.plusDays(1)))
                .isEqualTo(LocalDateTime.of(2029, 2, 28, 9, 0));
    }

    // ---------- nextAfter：过期滚动 ----------

    @Test
    @DisplayName("nextAfter：每月提醒长期过期后补完成，滚动到下一期（9/1 基准、9/20 完成 → 10/1）")
    void nextAfter_monthly_overdue_shouldRollForward() {
        // 每月 1 号提醒，计划 8/1，拖到 9/20 才完成
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime next = service.nextAfter(base, IntervalType.MONTHLY, 1, now);

        // 首算 9/1 已过期 → 以当前时间为基准重算 → 10/1（时刻取完成时刻）
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    @DisplayName("nextAfter：每N天提醒过期后补完成，以当前时间为基准滚动")
    void nextAfter_days_overdue_shouldRollForward() {
        // 每 3 天提醒，计划 9/1，拖到 9/20 才完成（首算 9/4 已过期）
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime next = service.nextAfter(base, IntervalType.DAYS, 3, now);

        // 以 9/20 10:00 为基准 +3 天
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 9, 23, 10, 0));
    }

    @Test
    @DisplayName("nextAfter：推算结果未过期时不滚动")
    void nextAfter_notOverdue_shouldNotRoll() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 9, 0);
        // 按期完成（完成时间早于下期推算结果）
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 10, 0);

        LocalDateTime next = service.nextAfter(base, IntervalType.MONTHS, 1, now);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    // ---------- validateInterval ----------

    @Test
    @DisplayName("validateInterval：once/yearly 忽略 value（null 或任意值均合法）")
    void validateInterval_onceAndYearly_shouldIgnoreValue() {
        service.validateInterval(IntervalType.ONCE, null);
        service.validateInterval(IntervalType.ONCE, 99);
        service.validateInterval(IntervalType.YEARLY, null);
        service.validateInterval(IntervalType.YEARLY, 5);
    }

    @Test
    @DisplayName("validateInterval：days/months 的 value 必填且 ≥1")
    void validateInterval_daysAndMonths_shouldRequirePositiveValue() {
        service.validateInterval(IntervalType.DAYS, 1);
        service.validateInterval(IntervalType.MONTHS, 12);

        assertThatThrownBy(() -> service.validateInterval(IntervalType.DAYS, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.DAYS, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.MONTHS, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.MONTHS, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateInterval：weekly 的 value 必须在 1~7")
    void validateInterval_weekly_shouldRequireOneToSeven() {
        service.validateInterval(IntervalType.WEEKLY, 1);
        service.validateInterval(IntervalType.WEEKLY, 7);

        assertThatThrownBy(() -> service.validateInterval(IntervalType.WEEKLY, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.WEEKLY, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.WEEKLY, 8))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateInterval：monthly 的 value 必须在 1~31")
    void validateInterval_monthly_shouldRequireOneToThirtyOne() {
        service.validateInterval(IntervalType.MONTHLY, 1);
        service.validateInterval(IntervalType.MONTHLY, 31);

        assertThatThrownBy(() -> service.validateInterval(IntervalType.MONTHLY, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.MONTHLY, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateInterval(IntervalType.MONTHLY, 32))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
