package com.fafa.domain.model.event;

import com.fafa.domain.model.reminder.IntervalType;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderCompletion;
import com.fafa.domain.model.reminder.ReminderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事件记录单元测试
 *
 * @author FaFa Team
 * @since 3.0
 */
class EventRecordTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 22, 10, 0);
    private static final Long USER_ID = 100L;

    private Reminder reminderWithType(ReminderType type) {
        return Reminder.restore(10L, 1L, USER_ID, type, "接种狂犬疫苗", "备注内容",
                IntervalType.ONCE, null, LocalDateTime.of(2026, 8, 22, 9, 0),
                com.fafa.domain.model.reminder.ReminderStatus.PENDING,
                null, null, null, null, false, null, null);
    }

    private ReminderCompletion completionOf(Reminder reminder) {
        return ReminderCompletion.create(reminder, USER_ID, NOW, "完成备注",
                List.of("http://oss/photo1.jpg"));
    }

    @Test
    @DisplayName("fromReminder：字段映射正确（source/sourceId/eventType/title/content/eventDate/materialUrls）")
    void fromReminder_shouldMapFields() {
        Reminder reminder = reminderWithType(ReminderType.VACCINE);
        ReminderCompletion completion = completionOf(reminder);

        EventRecord event = EventRecord.fromReminder(reminder, completion);

        assertThat(event.getPetId()).isEqualTo(1L);
        assertThat(event.getUserId()).isEqualTo(USER_ID);
        assertThat(event.getSource()).isEqualTo(EventSource.REMINDER);
        assertThat(event.getSourceId()).isEqualTo(10L);
        assertThat(event.getEventType()).isEqualTo(EventType.VACCINE);
        assertThat(event.getTitle()).isEqualTo("接种狂犬疫苗");
        assertThat(event.getContent()).isEqualTo("完成备注");
        assertThat(event.getEventDate()).isEqualTo(NOW.toLocalDate());
        assertThat(event.getMaterialUrls()).containsExactly("http://oss/photo1.jpg");
        assertThat(event.getOperatorId()).isEqualTo(USER_ID);
        assertThat(event.getTags()).isNull();
        assertThat(event.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("fromReminder：各生成事件类型映射正确")
    void fromReminder_shouldMapEventTypes() {
        assertThat(EventRecord.fromReminder(reminderWithType(ReminderType.DEWORMING),
                completionOf(reminderWithType(ReminderType.DEWORMING))).getEventType())
                .isEqualTo(EventType.DEWORMING);
        assertThat(EventRecord.fromReminder(reminderWithType(ReminderType.BATH),
                completionOf(reminderWithType(ReminderType.BATH))).getEventType())
                .isEqualTo(EventType.BATH);
        assertThat(EventRecord.fromReminder(reminderWithType(ReminderType.NAIL_TRIM),
                completionOf(reminderWithType(ReminderType.NAIL_TRIM))).getEventType())
                .isEqualTo(EventType.NAIL_TRIM);
        assertThat(EventRecord.fromReminder(reminderWithType(ReminderType.CHECKUP),
                completionOf(reminderWithType(ReminderType.CHECKUP))).getEventType())
                .isEqualTo(EventType.CHECKUP);
        assertThat(EventRecord.fromReminder(reminderWithType(ReminderType.CUSTOM),
                completionOf(reminderWithType(ReminderType.CUSTOM))).getEventType())
                .isEqualTo(EventType.CUSTOM);
    }

    @Test
    @DisplayName("fromReminder：非白名单类型抛出异常")
    void fromReminder_nonEventGeneratingType_shouldThrow() {
        Reminder birthday = reminderWithType(ReminderType.BIRTHDAY);

        assertThatThrownBy(() -> EventRecord.fromReminder(birthday, completionOf(birthday)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不生成事件记录");
    }

    @Test
    @DisplayName("fromReminder：空参数抛出 NPE")
    void fromReminder_shouldRejectNull() {
        Reminder reminder = reminderWithType(ReminderType.VACCINE);

        assertThatThrownBy(() -> EventRecord.fromReminder(null, completionOf(reminder)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reminder");

        assertThatThrownBy(() -> EventRecord.fromReminder(reminder, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("completion");
    }

    @Test
    @DisplayName("手动事件可创建、编辑、删除；提醒事件不可编辑删除")
    void manualEvent_shouldBeEditableOnlyForManualSource() {
        EventRecord manual = EventRecord.createManual(1L, USER_ID, EventType.CUSTOM, "第一次散步", "记录",
                LocalDate.of(2026, 8, 22), List.of("photo.jpg"), List.of("户外"), NOW, LocalDate.of(2026, 8, 23));
        assertThat(manual.isManual()).isTrue();
        manual.updateManual(EventType.BATH, "洗澡", null, LocalDate.of(2026, 8, 23), null,
                List.of("护理"), NOW.plusMinutes(1), LocalDate.of(2026, 8, 23));
        assertThat(manual.getEventType()).isEqualTo(EventType.BATH);
        manual.deleteManual();
        assertThat(manual.isDeleted()).isTrue();

        EventRecord reminder = EventRecord.fromReminder(reminderWithType(ReminderType.VACCINE), completionOf(reminderWithType(ReminderType.VACCINE)));
        assertThatThrownBy(() -> reminder.deleteManual()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reminder.updateManual(EventType.BATH, "x", null, LocalDate.now(), null, null,
                NOW, LocalDate.now())).isInstanceOf(IllegalStateException.class);
    }
}
