package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒完成历史持久化对象（追加只写，无逻辑删除）
 *
 * @author FaFa Team
 * @since 3.0
 */
@Data
@TableName("reminder_completion_history")
public class ReminderCompletionHistoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reminderId;

    private Long petId;

    /** 完成人ID（家庭协同预留，现阶段=宠物主人） */
    private Long userId;

    /** 提醒标题快照（提醒删除后历史仍可独立展示） */
    private String reminderTitle;

    /** 提醒类型快照 */
    private String reminderType;

    private LocalDateTime completionTime;

    private String notes;

    /** 完成照片URL列表（JSON 数组文本） */
    private String photoUrls;

    /** 关联生成的事件记录ID */
    private Long eventRecordId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
