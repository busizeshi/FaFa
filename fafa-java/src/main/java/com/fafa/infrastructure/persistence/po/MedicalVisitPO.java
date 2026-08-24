package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medical_visit")
public class MedicalVisitPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private LocalDate visitDate;
    private String hospitalName;
    private String doctorName;
    private String chiefComplaint;
    private String diagnosis;
    private String prescription;
    private String treatment;
    private BigDecimal totalCost;
    private String notes;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
