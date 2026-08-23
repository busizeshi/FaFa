package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重记录持久化对象
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@TableName("weight_record")
public class WeightRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private LocalDate recordDate;

    /** 体重（kg） */
    private BigDecimal weight;

    private String notes;

    private Long operatorId;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
