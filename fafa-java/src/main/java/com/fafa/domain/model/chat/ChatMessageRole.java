package com.fafa.domain.model.chat;

/** AI 会话消息角色。 */
public enum ChatMessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    SYSTEM("system");

    private final String code;

    ChatMessageRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChatMessageRole fromCode(String code) {
        for (ChatMessageRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知消息角色: " + code);
    }
}
