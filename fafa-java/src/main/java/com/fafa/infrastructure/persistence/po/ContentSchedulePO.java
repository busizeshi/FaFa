package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_schedule")
public class ContentSchedulePO {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long petId;
    private String contentType;
    private String topicType;
    private Integer intervalDays;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private Boolean enabled;
    @TableField("is_deleted") @TableLogic private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
