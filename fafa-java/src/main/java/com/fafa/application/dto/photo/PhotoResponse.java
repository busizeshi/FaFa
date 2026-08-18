package com.fafa.application.dto.photo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 照片响应
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
public class PhotoResponse {

    private Long id;
    private Long petId;
    private String url;
    private String thumbnailUrl;
    private String originalUrl;
    private LocalDateTime takenAt;
    private LocalDateTime uploadAt;
    private String description;
    private List<String> tags;
    private List<String> aiTags;
    private String aiDescription;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Boolean isCover;
    private LocalDateTime createdAt;
}
