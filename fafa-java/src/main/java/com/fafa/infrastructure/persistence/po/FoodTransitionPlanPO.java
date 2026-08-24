package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 换粮计划持久化对象。 */
@Data
@TableName("food_transition_plan")
public class FoodTransitionPlanPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private Long oldFoodId;
    private Long newFoodId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyTargetAmount;
    private String unit;
    private String ratioSteps;
    private LocalDate reactionDate;
    private String reactionCode;
    private String reactionNote;
    private String status;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
