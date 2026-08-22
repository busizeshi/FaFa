package com.fafa.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DO 基类：雪花 ID + 创建/更新时间自动填充
 *
 * 需要逻辑删除的表继承 {@link SoftDeleteDO}。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
public abstract class BaseDO {

    /** 雪花 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间（插入自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入/更新自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
