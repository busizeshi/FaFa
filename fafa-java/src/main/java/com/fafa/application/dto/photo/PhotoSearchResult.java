package com.fafa.application.dto.photo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 照片/视频语义搜索结果
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
public class PhotoSearchResult {

    private Long photoId;

    /**
     * 宠物 ID（未识别到宠物时为 null）
     */
    private Long petId;

    private String url;

    /**
     * 媒体类型: image 或 video
     */
    private String mediaType;

    private List<String> tags;

    /**
     * 相似度得分 (0-1)
     */
    private Double score;
}
