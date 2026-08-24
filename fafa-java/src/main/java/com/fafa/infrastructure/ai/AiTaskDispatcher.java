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

    /**
     * 清理宠物全部照片向量
     *
     * 删除宠物时调用，由 Python 按 pet_id 删除 Qdrant fafa_pet_profiles 中的全部向量。
     * 删除操作轻量且幂等，两种环境均直接 HTTP 调用 Python，无需走消息队列。
     *
     * @param petId 宠物ID
     */
    void cleanupPetVectors(Long petId);

    /**
     * 分发素材 AI 分析任务（照片/视频）
     *
     * 素材已上传到对象存储，此处只传 URL。Python 端完成：
     * qwen-vl 生成描述与标签 → qwen3-vl-embedding 向量化入 Qdrant fafa_media
     * → 与 fafa_pet_profiles 比对识别归属 → 回调 Java。
     *
     * @param mediaId   素材ID
     * @param userId    用户ID
     * @param url       素材对象存储URL
     * @param mediaType 媒体类型（photo/video）
     * @param tags      用户标签（可为空）
     * @param petId     用户指定归属宠物（可为空，表示待识别）
     * @param takenAt   素材拍摄时间，用于 Qdrant 时间范围过滤
     */
    void dispatchMediaAnalysis(Long mediaId, Long userId, String url,
                               String mediaType, java.util.List<String> tags, Long petId,
                               java.time.LocalDateTime takenAt);

    /**
     * 删除素材向量
     *
     * 删除素材时调用，由 Python 按 embedding_id（media_{mediaId}）删除 Qdrant fafa_media 中的向量。
     * 删除操作轻量且幂等，两种环境均直接 HTTP 调用 Python，无需走消息队列。
     *
     * @param mediaId 素材ID
     */
    void deleteMediaVector(Long mediaId);
}
