package com.fafa.domain.model.event;

import lombok.Getter;

/**
 * 事件类型（成长档案）
 *
 * Phase 3 由提醒完成写入；Phase 4+ 成长档案可扩展更多类型（如 first_walk、换粮等）。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public enum EventType {

    VACCINE("vaccine", "疫苗"),
    DEWORMING("deworming", "驱虫"),
    BATH("bath", "洗澡"),
    NAIL_TRIM("nail_trim", "剪指甲"),
    CHECKUP("checkup", "体检"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String label;

    EventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static EventType fromCode(String code) {
        for (EventType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid event type: " + code);
    }
}
