package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喂食记录持久化对象
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@TableName("feeding_record")
public class FeedingRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private LocalDateTime feedingTime;

    /** 食物类型: main_food/snack/can/other */
    private String foodType;

    private BigDecimal amount;

    /** 单位: g/ml */
    private String unit;

    /** 关联食品管理卡ID */
    private Long foodId;

    private Long feedingPlanId;

    private Long feedingPlanItemId;

    private LocalDateTime plannedAt;

    private String foodNameSnapshot;

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
