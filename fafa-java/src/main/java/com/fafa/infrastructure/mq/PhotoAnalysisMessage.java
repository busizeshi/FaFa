package com.fafa.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 照片分析消息
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
     * 照片ID
     */
    private Long photoId;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 照片URL
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 拍摄时间
     */
    private String takenAt;
}
