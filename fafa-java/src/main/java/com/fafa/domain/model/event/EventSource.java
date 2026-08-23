package com.fafa.domain.model.event;

import lombok.Getter;

/**
 * 事件来源
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public enum EventSource {

    MANUAL("manual", "手动创建"),
    REMINDER("reminder", "提醒完成生成");

    private final String code;
    private final String label;

    EventSource(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static EventSource fromCode(String code) {
        for (EventSource value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid event source: " + code);
    }
}
