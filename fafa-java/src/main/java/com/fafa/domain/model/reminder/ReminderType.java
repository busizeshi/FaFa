package com.fafa.domain.model.reminder;

import lombok.Getter;

/**
 * 提醒类型
 *
 * 事件生成白名单：vaccine/deworming/bath/nail_trim/checkup/custom 完成时生成事件记录；
 * birthday/daily_record 为纯提醒；medication 留待 Phase 6 写用药记录。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public enum ReminderType {

    VACCINE("vaccine", "疫苗"),
    DEWORMING("deworming", "驱虫"),
    BATH("bath", "洗澡"),
    NAIL_TRIM("nail_trim", "剪指甲"),
    CHECKUP("checkup", "体检"),
    MEDICATION("medication", "用药"),
    BIRTHDAY("birthday", "生日"),
    DAILY_RECORD("daily_record", "每日打卡"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String label;

    ReminderType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ReminderType fromCode(String code) {
        for (ReminderType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid reminder type: " + code);
    }

    /**
     * 完成时是否生成事件记录
     *
     * birthday/daily_record 为纯提醒不生成；medication 由 Phase 6 写用药记录。
     */
    public boolean generatesEvent() {
        return this == VACCINE || this == DEWORMING || this == BATH
                || this == NAIL_TRIM || this == CHECKUP || this == CUSTOM;
    }

    /**
     * 是否为重要提醒（提前 1 天推送）
     */
    public boolean isImportant() {
        return this == VACCINE || this == CHECKUP || this == DEWORMING;
    }
}
