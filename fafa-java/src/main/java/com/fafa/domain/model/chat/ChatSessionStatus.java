package com.fafa.domain.model.chat;

/** AI 会话状态。 */
public enum ChatSessionStatus {
    ACTIVE("active"),
    DELETED("deleted");

    private final String code;

    ChatSessionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChatSessionStatus fromCode(String code) {
        for (ChatSessionStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知会话状态: " + code);
    }
}
