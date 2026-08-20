package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签数据对象
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Data
@TableName("user_tag")
public class UserTagDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tagName;

    private String category;

    private Integer usageCount;

    private LocalDateTime lastUsedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
