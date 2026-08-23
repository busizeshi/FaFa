package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签持久化对象（字典数据，物理删除，无逻辑删除标记）
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@TableName("user_tag")
public class UserTagPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tagName;

    /** 分类: pet/location/action/object/mood/other */
    private String category;

    /** 使用次数（打标时+1，取消打标不回退） */
    private Integer usageCount;

    private LocalDateTime lastUsedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
