package com.fafa.domain.model.photo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 照片聚合根
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
public class Photo implements Serializable {

    private PhotoId photoId;
    private Long petId;
    private Long userId;
    private String url;
    private String thumbnailUrl;
    private String originalUrl;
    private LocalDateTime takenAt;
    private LocalDateTime uploadAt;
    private String description;
    private List<String> tags;
    private List<String> aiTags;
    private String aiDescription;
    private String embeddingId;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Boolean isCover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建照片
     */
    public static Photo create(Long petId, Long userId, String url, String thumbnailUrl,
                               LocalDateTime takenAt, String description) {
        return Photo.builder()
                .petId(petId)
                .userId(userId)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .takenAt(takenAt)
                .uploadAt(LocalDateTime.now())
                .description(description)
                .isCover(false)
                .build();
    }

    /**
     * 更新 AI 分析结果
     */
    public void updateAiAnalysis(List<String> aiTags, String aiDescription, String embeddingId) {
        this.aiTags = aiTags;
        this.aiDescription = aiDescription;
        this.embeddingId = embeddingId;
    }

    /**
     * 更新用户描述
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 设置为封面
     */
    public void setAsCover() {
        this.isCover = true;
    }

    /**
     * 取消封面
     */
    public void unsetCover() {
        this.isCover = false;
    }

    /**
     * 更新图片元数据
     */
    public void updateMetadata(Integer width, Integer height, Long fileSize, String originalUrl) {
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
        this.originalUrl = originalUrl;
    }
}
