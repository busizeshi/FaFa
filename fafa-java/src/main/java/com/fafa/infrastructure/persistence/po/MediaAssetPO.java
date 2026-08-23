package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 媒体素材持久化对象
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@TableName("media_asset")
public class MediaAssetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 归属宠物ID（用户指定或AI识别判定，待用户确认） */
    private Long petId;

    /** 媒体类型: photo/video */
    private String mediaType;

    private String fileUrl;

    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    private String mimeType;

    /** 标签名数组（JSON 文本） */
    private String tags;

    /** AI分析状态: pending/processing/completed/failed/skipped */
    private String status;

    private String aiDescription;

    /** AI生成标签（JSON 文本） */
    private String aiTags;

    /** 宠物识别相似度（cosine 0~1；手动指定归属时为 NULL） */
    private BigDecimal recognitionScore;

    /** 归属是否已确认 */
    private Boolean recognitionConfirmed;

    private LocalDateTime takenAt;

    private String errorMessage;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
