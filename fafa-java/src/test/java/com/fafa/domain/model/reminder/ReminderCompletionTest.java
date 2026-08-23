package com.fafa.domain.model.reminder;

import com.fafa.domain.service.ReminderScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 提醒完成记录单元测试
 *
 * @author FaFa Team
 * @since 3.0
 */
class ReminderCompletionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 22, 10, 0);
    private static final Long USER_ID = 100L;

    private Reminder newCompletedReminder() {
        Reminder reminder = Reminder.create(1L, USER_ID, ReminderType.VACCINE, "接种狂犬疫苗",
                null, IntervalType.ONCE, null, LocalDateTime.of(2026, 8, 22, 9, 0), NOW.minusDays(1));
        // 模拟已保存（有 ID）
        return Reminder.restore(10L, reminder.getPetId(), reminder.getUserId(), reminder.getType(),
                reminder.getTitle(), reminder.getDescription(), reminder.getIntervalType(),
                reminder.getIntervalValue(), reminder.getNextRemindTime(), reminder.getStatus(),
                null, null, null, null, false, null, null);
    }

    @Test
    @DisplayName("create：快照字段取自提醒（标题/类型）")
    void create_shouldSnapshotFromReminder() {
        Reminder reminder = newCompletedReminder();

        ReminderCompletion completion = ReminderCompletion.create(
                reminder, USER_ID, NOW, "已完成", List.of("http://oss/photo1.jpg"));

        assertThat(completion.getReminderId()).isEqualTo(10L);
        assertThat(completion.getPetId()).isEqualTo(1L);
        assertThat(completion.getUserId()).isEqualTo(USER_ID);
        assertThat(completion.getReminderTitle()).isEqualTo("接种狂犬疫苗");
        assertThat(completion.getReminderType()).isEqualTo("vaccine");
        assertThat(completion.getCompletionTime()).isEqualTo(NOW);
        assertThat(completion.getNotes()).isEqualTo("已完成");
        assertThat(completion.getPhotoUrls()).containsExactly("http://oss/photo1.jpg");
        assertThat(completion.getEventRecordId()).isNull();
    }

    @Test
    @DisplayName("create：必填字段为空抛出 NPE")
    void create_shouldRejectNullRequiredFields() {
        Reminder reminder = newCompletedReminder();

        assertThatThrownBy(() -> ReminderCompletion.create(null, USER_ID, NOW, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reminder");

        assertThatThrownBy(() -> ReminderCompletion.create(reminder, null, NOW, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");

        assertThatThrownBy(() -> ReminderCompletion.create(reminder, USER_ID, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("completionTime");
    }

    @Test
    @DisplayName("create：备注超长抛出异常")
    void create_shouldRejectLongNotes() {
        Reminder reminder = newCompletedReminder();

        assertThatThrownBy(() -> ReminderCompletion.create(reminder, USER_ID, NOW, "a".repeat(501), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("备注不能超过");
    }

    @Test
    @DisplayName("create：照片超过9张抛出异常")
    void create_shouldRejectTooManyPhotos() {
        Reminder reminder = newCompletedReminder();
        List<String> photos = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

        assertThatThrownBy(() -> ReminderCompletion.create(reminder, USER_ID, NOW, null, photos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("照片不能超过");
    }

    @Test
    @DisplayName("assignEventRecordId：回填事件记录ID")
    void assignEventRecordId_shouldSetId() {
        Reminder reminder = newCompletedReminder();
        ReminderCompletion completion = ReminderCompletion.create(reminder, USER_ID, NOW, null, null);

        completion.assignEventRecordId(99L);

        assertThat(completion.getEventRecordId()).isEqualTo(99L);
    }
}
