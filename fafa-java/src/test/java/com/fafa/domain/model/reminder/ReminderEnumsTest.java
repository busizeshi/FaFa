package com.fafa.domain.model.reminder;

import com.fafa.domain.model.event.EventSource;
import com.fafa.domain.model.event.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 3 领域枚举单元测试
 *
 * @author FaFa Team
 * @since 3.0
 */
class ReminderEnumsTest {

    // ---------- ReminderType ----------

    @Test
    @DisplayName("ReminderType.generatesEvent：白名单矩阵")
    void generatesEvent_matrix() {
        assertThat(ReminderType.VACCINE.generatesEvent()).isTrue();
        assertThat(ReminderType.DEWORMING.generatesEvent()).isTrue();
        assertThat(ReminderType.BATH.generatesEvent()).isTrue();
        assertThat(ReminderType.NAIL_TRIM.generatesEvent()).isTrue();
        assertThat(ReminderType.CHECKUP.generatesEvent()).isTrue();
        assertThat(ReminderType.CUSTOM.generatesEvent()).isTrue();

        assertThat(ReminderType.BIRTHDAY.generatesEvent()).isFalse();
        assertThat(ReminderType.DAILY_RECORD.generatesEvent()).isFalse();
        assertThat(ReminderType.MEDICATION.generatesEvent()).isFalse();
    }

    @Test
    @DisplayName("ReminderType.isImportant：疫苗/体检/驱虫提前1天推送")
    void isImportant_matrix() {
        assertThat(ReminderType.VACCINE.isImportant()).isTrue();
        assertThat(ReminderType.CHECKUP.isImportant()).isTrue();
        assertThat(ReminderType.DEWORMING.isImportant()).isTrue();

        assertThat(ReminderType.BATH.isImportant()).isFalse();
        assertThat(ReminderType.NAIL_TRIM.isImportant()).isFalse();
        assertThat(ReminderType.MEDICATION.isImportant()).isFalse();
        assertThat(ReminderType.BIRTHDAY.isImportant()).isFalse();
        assertThat(ReminderType.DAILY_RECORD.isImportant()).isFalse();
        assertThat(ReminderType.CUSTOM.isImportant()).isFalse();
    }

    // ---------- fromCode：五枚举大小写不敏感与非法值 ----------

    @Test
    @DisplayName("fromCode：大小写不敏感")
    void fromCode_shouldBeCaseInsensitive() {
        assertThat(ReminderType.fromCode("VACCINE")).isEqualTo(ReminderType.VACCINE);
        assertThat(ReminderType.fromCode("Daily_Record")).isEqualTo(ReminderType.DAILY_RECORD);
        assertThat(IntervalType.fromCode("WEEKLY")).isEqualTo(IntervalType.WEEKLY);
        assertThat(ReminderStatus.fromCode("Pending")).isEqualTo(ReminderStatus.PENDING);
        assertThat(EventType.fromCode("NAIL_TRIM")).isEqualTo(EventType.NAIL_TRIM);
        assertThat(EventSource.fromCode("REMINDER")).isEqualTo(EventSource.REMINDER);
    }

    @Test
    @DisplayName("fromCode：非法值抛出异常")
    void fromCode_invalidValue_shouldThrow() {
        assertThatThrownBy(() -> ReminderType.fromCode("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalType.fromCode("daily"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReminderStatus.fromCode("completed"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventType.fromCode("birthday"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventSource.fromCode("system"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("枚举取值完整性：9 种提醒类型、6 种周期、3 种状态、6 种事件类型、2 种来源")
    void enumValues_completeness() {
        assertThat(ReminderType.values()).hasSize(9);
        assertThat(IntervalType.values()).hasSize(6);
        assertThat(ReminderStatus.values()).hasSize(3);
        assertThat(EventType.values()).hasSize(6);
        assertThat(EventSource.values()).hasSize(2);
    }
}
