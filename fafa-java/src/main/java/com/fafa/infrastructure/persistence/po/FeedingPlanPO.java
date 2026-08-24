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

/** 喂食计划持久化对象。 */
@Data
@TableName("feeding_plan")
public class FeedingPlanPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyTargetCalories;
    private BigDecimal dailyTargetAmount;
    private String unit;
    private String recommendationSource;
    private String policyVersion;
    private Integer version;
    private String status;
    private LocalDateTime acceptedAt;
    private String notes;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
