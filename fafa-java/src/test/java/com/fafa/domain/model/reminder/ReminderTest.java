package com.fafa.domain.model.reminder;

import com.fafa.domain.service.ReminderScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 提醒聚合根单元测试
 *
 * @author FaFa Team
 * @since 3.0
 */
class ReminderTest {

    private static final Long PET_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 22, 10, 0);
    private static final LocalDateTime FUTURE_TIME = LocalDateTime.of(2026, 9, 1, 9, 0);

    private final ReminderScheduleService schedule = new ReminderScheduleService();

    private Reminder newReminder(IntervalType intervalType, Integer intervalValue) {
        return Reminder.create(PET_ID, USER_ID, ReminderType.VACCINE, "接种狂犬疫苗",
                "第三针", intervalType, intervalValue, FUTURE_TIME, NOW);
    }

    // ---------- create ----------

    @Test
    @DisplayName("create：正常创建，状态为 pending")
    void create_shouldInitPendingStatus() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reminder.getType()).isEqualTo(ReminderType.VACCINE);
        assertThat(reminder.isDeleted()).isFalse();
        assertThat(reminder.getLastCompletionTime()).isNull();
    }

    @Test
    @DisplayName("create：必填字段为空抛出 NPE")
    void create_shouldRejectNullRequiredFields() {
        assertThatThrownBy(() -> Reminder.create(null, USER_ID, ReminderType.BATH, "洗澡",
                null, IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("petId");

        assertThatThrownBy(() -> Reminder.create(PET_ID, null, ReminderType.BATH, "洗澡",
                null, IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");

        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, null, "洗澡",
                null, IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");

        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, "洗澡",
                null, null, null, FUTURE_TIME, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("intervalType");

        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, "洗澡",
                null, IntervalType.ONCE, null, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("nextRemindTime");
    }

    @Test
    @DisplayName("create：标题为空或超长抛出异常")
    void create_shouldValidateTitle() {
        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, null,
                null, IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题不能为空");

        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, "a".repeat(201),
                null, IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题不能超过");
    }

    @Test
    @DisplayName("create：描述超长抛出异常")
    void create_shouldRejectLongDescription() {
        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, "洗澡",
                "a".repeat(501), IntervalType.ONCE, null, FUTURE_TIME, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("描述不能超过");
    }

    @Test
    @DisplayName("create：提醒时间不晚于当前时间抛出异常")
    void create_shouldRejectPastRemindTime() {
        assertThatThrownBy(() -> Reminder.create(PET_ID, USER_ID, ReminderType.BATH, "洗澡",
                null, IntervalType.ONCE, null, NOW.minusHours(1), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("提醒时间不能早于当前时间");
    }

    // ---------- complete ----------

    @Test
    @DisplayName("complete：一次性提醒完成后进入 finished 终态")
    void complete_once_shouldFinish() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);

        reminder.complete(FUTURE_TIME.plusHours(1), "已完成", schedule);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.FINISHED);
        assertThat(reminder.getLastCompletionTime()).isEqualTo(FUTURE_TIME.plusHours(1));
        assertThat(reminder.getLastCompletionNotes()).isEqualTo("已完成");
    }

    @Test
    @DisplayName("complete：周期性提醒完成后推进 nextRemindTime 且保持 pending")
    void complete_periodic_shouldAdvanceAndStayPending() {
        Reminder reminder = newReminder(IntervalType.MONTHS, 3);

        reminder.complete(FUTURE_TIME.plusDays(2), null, schedule);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        // 以被完成期计划时间（9/1）为基准 +3 个月 = 12/1，而非以完成时间 9/3 为基准
        assertThat(reminder.getNextRemindTime()).isEqualTo(LocalDateTime.of(2026, 12, 1, 9, 0));
    }

    @Test
    @DisplayName("complete：长期过期后补完成，过期滚动到未来时间")
    void complete_overdue_shouldRollForward() {
        Reminder reminder = newReminder(IntervalType.MONTHLY, 1);

        // 计划 9/1，拖到 10/15 才完成：首算 10/1 已过期 → 以完成时间重算 → 11/1（时刻取完成时刻）
        reminder.complete(LocalDateTime.of(2026, 10, 15, 10, 0), null, schedule);

        assertThat(reminder.getNextRemindTime()).isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
    }

    @Test
    @DisplayName("complete：非 pending 状态完成抛出异常")
    void complete_nonPending_shouldThrow() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);
        reminder.complete(FUTURE_TIME.plusHours(1), null, schedule);

        assertThatThrownBy(() -> reminder.complete(FUTURE_TIME.plusDays(1), null, schedule))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仅生效中的提醒可以完成");
    }

    @Test
    @DisplayName("complete：备注超长抛出异常")
    void complete_shouldRejectLongNotes() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);

        assertThatThrownBy(() -> reminder.complete(FUTURE_TIME.plusHours(1), "a".repeat(501), schedule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("备注不能超过");
    }

    // ---------- update ----------

    @Test
    @DisplayName("update：覆盖编辑字段且保持状态不变")
    void update_shouldOverwriteFieldsAndKeepStatus() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);
        reminder.pause();

        LocalDateTime newTime = LocalDateTime.of(2026, 10, 1, 9, 0);
        reminder.update(ReminderType.DEWORMING, "体内驱虫", "拜宠清",
                IntervalType.MONTHS, 3, newTime, NOW);

        assertThat(reminder.getType()).isEqualTo(ReminderType.DEWORMING);
        assertThat(reminder.getTitle()).isEqualTo("体内驱虫");
        assertThat(reminder.getIntervalType()).isEqualTo(IntervalType.MONTHS);
        assertThat(reminder.getIntervalValue()).isEqualTo(3);
        assertThat(reminder.getNextRemindTime()).isEqualTo(newTime);
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PAUSED);
    }

    @Test
    @DisplayName("update：新提醒时间不晚于当前时间抛出异常")
    void update_shouldRejectPastRemindTime() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);

        assertThatThrownBy(() -> reminder.update(ReminderType.BATH, "洗澡", null,
                IntervalType.ONCE, null, NOW.minusHours(1), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("提醒时间不能早于当前时间");
    }

    // ---------- pause / resume ----------

    @Test
    @DisplayName("pause/resume：pending → paused → pending")
    void pauseAndResume_shouldTransitionCorrectly() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);

        reminder.pause();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PAUSED);

        reminder.resume();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    @DisplayName("pause：非 pending 状态暂停抛出异常")
    void pause_nonPending_shouldThrow() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);
        reminder.complete(FUTURE_TIME.plusHours(1), null, schedule);

        assertThatThrownBy(reminder::pause)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仅生效中的提醒可以暂停");
    }

    @Test
    @DisplayName("resume：非 paused 状态恢复抛出异常")
    void resume_nonPaused_shouldThrow() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);

        assertThatThrownBy(reminder::resume)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仅已暂停的提醒可以恢复");
    }

    @Test
    @DisplayName("resume：恢复后 nextRemindTime 不重算（暂停期间过期则恢复后即为逾期）")
    void resume_shouldNotRecalculateNextTime() {
        Reminder reminder = newReminder(IntervalType.YEARLY, null);
        LocalDateTime original = reminder.getNextRemindTime();

        reminder.pause();
        reminder.resume();

        assertThat(reminder.getNextRemindTime()).isEqualTo(original);
    }

    // ---------- isOverdue / markPushed / delete ----------

    @Test
    @DisplayName("isOverdue：pending 且提醒时间已过为逾期")
    void isOverdue_shouldCheckStatusAndTime() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);

        assertThat(reminder.isOverdue(FUTURE_TIME.minusHours(1))).isFalse();
        assertThat(reminder.isOverdue(FUTURE_TIME.plusHours(1))).isTrue();

        reminder.pause();
        assertThat(reminder.isOverdue(FUTURE_TIME.plusHours(1))).isFalse();
    }

    @Test
    @DisplayName("markPushed：记录推送时间")
    void markPushed_shouldRecordPushTime() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);
        LocalDateTime pushTime = NOW.plusHours(2);

        reminder.markPushed(pushTime);

        assertThat(reminder.getLastPushTime()).isEqualTo(pushTime);
    }

    @Test
    @DisplayName("delete：逻辑删除标记")
    void delete_shouldMarkDeleted() {
        Reminder reminder = newReminder(IntervalType.ONCE, null);
        reminder.delete();
        assertThat(reminder.isDeleted()).isTrue();
    }
}
