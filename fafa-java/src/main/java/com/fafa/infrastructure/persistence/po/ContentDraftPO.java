package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("content_draft")
public class ContentDraftPO {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long petId;
    private String contentType;
    private String topicType;
    private String customTopic;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String selectedMediaJson;
    private String factSnapshotJson;
    private String draftJson;
    private String status;
    private String contentMode;
    private String generationTrigger;
    private String idempotencyKey;
    private Integer generationVersion;
    private String modelName;
    private String promptVersion;
    private String lastErrorCode;
    private String lastErrorMessage;
    @TableField("is_deleted") @TableLogic private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
