package com.fafa.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 照片/视频分析消息
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAnalysisMessage implements Serializable {

    /**
     * 照片/视频ID
     */
    private Long photoId;

    /**
     * 宠物ID (可选)
     */
    private Long petId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 照片/视频URL
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 媒体类型: image 或 video
     */
    private String mediaType;

    /**
     * 拍摄时间
     */
    private String takenAt;

    /**
     * 用户标签
     */
    private List<String> tags;
}
