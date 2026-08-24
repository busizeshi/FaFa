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

/** 食品管理卡持久化对象。 */
@Data
@TableName("food_card")
public class FoodCardPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private String name;
    private String brand;
    private String foodType;
    private String specification;
    private BigDecimal packageWeight;
    private String packageWeightUnit;
    private BigDecimal price;
    private String currency;
    private LocalDate openedAt;
    private LocalDate expiresAt;
    private String nutritionBasis;
    private BigDecimal proteinPercent;
    private BigDecimal fatPercent;
    private BigDecimal fiberPercent;
    private BigDecimal moisturePercent;
    private BigDecimal ashPercent;
    private BigDecimal calciumPercent;
    private BigDecimal phosphorusPercent;
    private BigDecimal energyKcal;
    private String energyUnit;
    private String proteinSources;
    private String ingredients;
    private String additiveFlags;
    private String lifeStageLabel;
    private Long sourceMediaId;
    private Long sourceRecognitionTaskId;
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
