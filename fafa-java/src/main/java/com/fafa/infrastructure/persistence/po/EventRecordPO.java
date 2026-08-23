package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 事件记录持久化对象
 *
 * @author FaFa Team
 * @since 3.0
 */
@Data
@TableName("event_record")
public class EventRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private Long userId;

    /** 来源: manual/reminder */
    private String source;

    /** 来源对象ID（source=reminder 时为 reminder_id） */
    private Long sourceId;

    /** 事件类型: vaccine/deworming/bath/nail_trim/checkup/custom */
    private String eventType;

    private String title;

    private String content;

    private LocalDate eventDate;

    /** 素材URL列表（JSON 数组文本） */
    private String materialUrls;

    /** 标签列表（JSON 数组文本，Phase 4 打通 user_tag） */
    private String tags;

    /** 记录人ID（家庭协同预留） */
    private Long operatorId;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
