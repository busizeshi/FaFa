package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pet_report")
public class PetReportPO {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long petId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String dataStatus;
    private String status;
    private String snapshotVersion;
    private String calculationVersion;
    private String factSnapshotJson;
    private String contentJson;
    private String contentMode;
    private String modelName;
    private String promptVersion;
    private String generationTrigger;
    private Integer generationVersion;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime generatedAt;
    @TableField("is_deleted") @TableLogic private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
