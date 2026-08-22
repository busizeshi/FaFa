package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物持久化对象
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@TableName("pet")
public class PetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** 宠物类型: cat/dog/other */
    private String type;

    private String breed;

    /** 性别: male/female/unknown */
    private String gender;

    private LocalDate birthDate;

    @TableField("is_neutered")
    private Boolean isNeutered;

    private LocalDate adoptDate;

    private BigDecimal weight;

    private String avatarUrl;

    private String frontPhotoUrl;

    private String sidePhotoUrl;

    private Integer sortOrder;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
