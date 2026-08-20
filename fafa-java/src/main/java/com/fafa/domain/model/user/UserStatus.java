package com.fafa.domain.model.user;

/**
 * 用户状态枚举
 * 
 * @author FaFa Team
 * @since 1.0
 */
public enum UserStatus {
    
    /**
     * 禁用
     */
    DISABLED(0, "禁用"),
    
    /**
     * 正常
     */
    NORMAL(1, "正常"),
    
    /**
     * 待注销（冷静期）
     */
    PENDING_DELETION(2, "待注销");
    
    private final Integer code;
    private final String description;
    
    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static UserStatus fromCode(Integer code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态: " + code);
    }
}
