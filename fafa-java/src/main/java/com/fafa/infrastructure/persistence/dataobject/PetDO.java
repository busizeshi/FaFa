package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物数据对象（DO）
 * 
 * 用于数据库持久化
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@TableName("pet")
public class PetDO {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 宠物昵称
     */
    private String name;

    /**
     * 宠物头像
     */
    private String avatar;

    /**
     * 种类
     */
    private String species;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别
     */
    private String gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 到家日期
     */
    private LocalDate adoptDate;

    /**
     * 当前体重
     */
    private Double weight;

    /**
     * 是否绝育
     */
    private Boolean isNeutered;

    /**
     * 毛色
     */
    private String coatColor;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer isDeleted;
}
