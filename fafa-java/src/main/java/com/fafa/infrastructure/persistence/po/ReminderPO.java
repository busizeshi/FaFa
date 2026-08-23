package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒持久化对象
 *
 * @author FaFa Team
 * @since 3.0
 */
@Data
@TableName("reminder")
public class ReminderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private Long userId;

    /** 提醒类型: vaccine/deworming/bath/nail_trim/checkup/medication/birthday/daily_record/custom */
    private String type;

    private String title;

    private String description;

    /** 周期类型: once/days/weekly/monthly/months/yearly */
    private String intervalType;

    /** 周期值语义随 intervalType */
    private Integer intervalValue;

    private LocalDateTime nextRemindTime;

    /** 状态: pending/paused/finished */
    private String status;

    private LocalDateTime lastCompletionTime;

    private String lastCompletionNotes;

    /** 最近一次推送时间（同一提醒每日至多推送一次） */
    private LocalDateTime lastPushTime;

    /** 关联用药ID（Phase 6 用药管理预留） */
    private Long medicationId;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
