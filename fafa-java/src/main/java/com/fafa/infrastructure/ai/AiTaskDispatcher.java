package com.fafa.infrastructure.ai;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI 任务分发策略接口
 *
 * 开发环境：Java HTTP 直接调用 Python
 * 生产环境：RocketMQ 异步消息
 *
 * @author FaFa Team
 * @since 1.0
 */
public interface AiTaskDispatcher {

    /**
     * 分发三视图向量化任务
     *
     * @param petId      宠物ID
     * @param frontPhoto 正面照
     * @param sidePhoto  侧面照
     * @param frontUrl   正面照URL
     * @param sideUrl    侧面照URL
     */
    void dispatchProfilePhotoVectorization(Long petId, MultipartFile frontPhoto, 
                                          MultipartFile sidePhoto, String frontUrl, 
                                          String sideUrl);
}
