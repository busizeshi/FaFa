package com.fafa.application.dto.photo;

import lombok.Builder;
import lombok.Data;

/**
 * 照片搜索结果
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
public class PhotoSearchResult {

    private Long photoId;
    private Long petId;
    private String url;
    private String thumbnailUrl;
    private String description;
    private String aiDescription;
    private Double score;
}
