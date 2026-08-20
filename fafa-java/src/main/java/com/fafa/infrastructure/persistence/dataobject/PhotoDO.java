package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 照片/视频数据对象
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@TableName("photo")
public class PhotoDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private Long userId;

    private String mediaType;

    private Boolean autoRecognized;

    private BigDecimal recognitionConfidence;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String recognizedPetIds;

    private String url;

    private String thumbnailUrl;

    private String videoCoverUrl;

    private String originalUrl;

    private LocalDateTime takenAt;

    private LocalDateTime uploadAt;

    private String description;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String tags;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String aiTags;

    private String aiDescription;

    private String embeddingId;

    private Integer width;

    private Integer height;

    private Long fileSize;

    private Integer duration;

    private Integer isCover;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
