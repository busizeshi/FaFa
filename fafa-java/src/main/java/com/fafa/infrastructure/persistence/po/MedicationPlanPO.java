package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medication_plan")
public class MedicationPlanPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private Long sourceVisitId;
    private String medicationName;
    private String specification;
    private String purpose;
    private BigDecimal dosageValue;
    private String dosageUnit;
    private String frequencyType;
    private Integer frequencyValue;
    private String administrationTimes;
    private String mealRelation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String notes;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
