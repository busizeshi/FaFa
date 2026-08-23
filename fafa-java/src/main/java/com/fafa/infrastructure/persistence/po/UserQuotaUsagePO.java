package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户配额使用记录持久化对象
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@TableName("user_quota_usage")
public class UserQuotaUsagePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 配额类型（Phase 4 使用 STORAGE_USED，单位字节） */
    private String quotaType;

    /** 已使用量 */
    private Long usedAmount;

    /** 上次重置时间（仅周期性配额） */
    private LocalDateTime lastResetTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
