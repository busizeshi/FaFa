package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 喂食计划项持久化对象。 */
@Data
@TableName("feeding_plan_item")
public class FeedingPlanItemPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long feedingPlanId;
    private String itemKey;
    private Long foodId;
    private LocalTime mealTime;
    private BigDecimal amount;
    private String unit;
    private BigDecimal percentage;
    private String cycleType;
    private String cycleValue;
    private String nutritionSnapshot;
    private Integer sortOrder;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
}
