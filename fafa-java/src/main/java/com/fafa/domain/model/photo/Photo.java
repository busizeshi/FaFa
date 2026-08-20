package com.fafa.domain.model.photo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 照片/视频聚合根
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
    private String mediaType;
    private Boolean autoRecognized;
    private BigDecimal recognitionConfidence;
    private List<Long> recognizedPetIds;
    private String url;
    private String thumbnailUrl;
    private String videoCoverUrl;
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
    private Integer duration;
    private Boolean isCover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建照片/视频
     */
    public static Photo create(Long petId, Long userId, String url, String thumbnailUrl,
                               LocalDateTime takenAt, String description, String mediaType) {
        return Photo.builder()
                .petId(petId)
                .userId(userId)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .takenAt(takenAt)
                .uploadAt(LocalDateTime.now())
                .description(description)
                .mediaType(mediaType)
                .autoRecognized(false)
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

    /**
     * 标记为自动识别
     */
    public void markAsAutoRecognized(Long petId, BigDecimal confidence) {
        this.petId = petId;
        this.autoRecognized = true;
        this.recognitionConfidence = confidence;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新视频信息
     */
    public void updateVideoInfo(Integer duration, String coverUrl) {
        this.duration = duration;
        this.videoCoverUrl = coverUrl;
    }

    /**
     * 设置识别出的宠物ID列表
     */
    public void setRecognizedPetIds(List<Long> recognizedPetIds) {
        this.recognizedPetIds = recognizedPetIds;
    }
}
