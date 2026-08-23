package com.fafa.domain.model.reminder;

import lombok.Getter;

/**
 * 提醒状态
 *
 * 无持久 completed 态：周期性提醒完成后原地推进 nextRemindTime 保持 pending，
 * 一次性提醒完成后进入 finished 终态；完成历史由 reminder_completion_history 独立承载。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public enum ReminderStatus {

    PENDING("pending", "生效中"),
    PAUSED("paused", "已暂停"),
    FINISHED("finished", "已结束");

    private final String code;
    private final String label;

    ReminderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ReminderStatus fromCode(String code) {
        for (ReminderStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid reminder status: " + code);
    }
}
