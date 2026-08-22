package com.fafa.infrastructure.ai;

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
     * 分发宠物照片向量化任务
     *
     * 照片已上传到对象存储，此处只传 URL，由 Python 调用 qwen3-vl-embedding 生成向量存入 Qdrant。
     * 三张照片（头像/正面/侧面）可全部或部分提供，Python 端按 view 分别 upsert，幂等。
     *
     * @param petId          宠物ID
     * @param userId         用户ID
     * @param avatarUrl      头像URL（可为null，表示本次不更新头像）
     * @param frontPhotoUrl  正面照URL（可为null）
     * @param sidePhotoUrl   侧面照URL（可为null）
     */
    void dispatchPetPhotoVectorization(Long petId, Long userId,
                                       String avatarUrl, String frontPhotoUrl, String sidePhotoUrl);
}
