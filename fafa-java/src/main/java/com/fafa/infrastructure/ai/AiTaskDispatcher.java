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
     * 分发素材 embedding 向量化任务（照片/视频）
     *
     * 素材已上传到对象存储，此处只传 URL。Python 端只调用 qwen3-vl-embedding，
     * 将向量写入 Qdrant fafa_media，并与 fafa_pet_profiles 的三视图向量比较后回调 Java。
     *
     * @param mediaId   素材ID
     * @param userId    用户ID
     * @param sourceUrl 素材在对象存储中的永久引用，写入 Qdrant
     * @param analysisUrl 本次推理使用的地址（视频通常为临时预签名 URL）
     * @param mediaType 媒体类型（photo/video）
     * @param tags      用户标签（可为空）
     * @param petId     用户指定或单宠物直接归属（可为空）
     * @param candidatePetIds 当前用户可参与匹配的宠物 ID；单宠物/指定宠物为空，多宠物为全部候选
     * @param takenAt   素材拍摄时间，用于 Qdrant 时间范围过滤
     * @param uploadedAt 素材上传入库时间，用于 Qdrant 上传时间追溯
     */
    void dispatchMediaAnalysis(Long mediaId, Long userId, String sourceUrl, String analysisUrl,
                               String mediaType, java.util.List<String> tags, Long petId,
                               java.util.List<Long> candidatePetIds,
                               java.time.LocalDateTime takenAt,
                               java.time.LocalDateTime uploadedAt);

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
