package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medication_record")
public class MedicationRecordPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private Long medicationPlanId;
    private Long reminderId;
    private LocalDateTime scheduledAt;
    private LocalDateTime takenAt;
    private BigDecimal dosageValue;
    private String dosageUnit;
    private String status;
    private String notes;
    private Long operatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
