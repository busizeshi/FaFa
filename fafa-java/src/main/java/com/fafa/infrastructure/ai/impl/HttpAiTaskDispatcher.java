package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 同步调用 AI 任务分发实现（开发环境）
 *
 * 照片已上传到对象存储，仅传 URL 到 Python 服务，
 * 由 Python 调用 qwen3-vl-embedding 生成向量并写入 Qdrant。
 * 复用统一配置的 pythonRestClient（内网令牌 + 120s 读超时覆盖 AI 推理耗时）。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.ai.dispatcher", havingValue = "http", matchIfMissing = true)
public class HttpAiTaskDispatcher implements AiTaskDispatcher {

    /** 按名称注入 pythonRestClient（区别于 wechatRestClient） */
    private final RestClient pythonRestClient;

    @Override
    public void dispatchPetPhotoVectorization(Long petId, Long userId,
                                              String avatarUrl, String frontPhotoUrl, String sidePhotoUrl) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pet_id", petId);
            body.put("user_id", userId);
            body.put("avatar_url", avatarUrl != null ? avatarUrl : "");
            body.put("front_url", frontPhotoUrl != null ? frontPhotoUrl : "");
            body.put("side_url", sidePhotoUrl != null ? sidePhotoUrl : "");

            pythonRestClient.post()
                    .uri("/api/pets/profile-photos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 宠物照片向量化任务已分发: petId={}, userId={}", petId, userId);

        } catch (Exception ex) {
            log.error("HTTP 分发宠物照片向量化失败: petId={}, userId={}", petId, userId, ex);
            // 不抛异常，避免影响主流程（照片已上传成功，向量化可重试）
        }
    }

    @Override
    public void cleanupPetVectors(Long petId) {
        try {
            pythonRestClient.delete()
                    .uri("/api/pets/{petId}/profile-vectors", petId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 宠物向量清理任务已分发: petId={}", petId);

        } catch (Exception ex) {
            log.error("HTTP 分发宠物向量清理失败: petId={}", petId, ex);
            // 不抛异常，避免影响删除主流程（残留向量可由对账任务兜底）
        }
    }

    @Override
    public void dispatchMediaAnalysis(Long mediaId, Long userId, String url,
                                      String mediaType, java.util.List<String> tags, Long petId,
                                      java.time.LocalDateTime takenAt) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("photo_id", mediaId);
            body.put("url", url);
            body.put("media_type", mediaType);
            body.put("tags", tags);
            body.put("pet_id", petId);
            body.put("taken_at", takenAt);
            body.put("message_id", "media-" + mediaId);

            pythonRestClient.post()
                    .uri("/api/photos/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 素材分析任务已分发: mediaId={}, userId={}", mediaId, userId);

        } catch (Exception ex) {
            log.error("HTTP 分发素材分析失败: mediaId={}, userId={}", mediaId, userId, ex);
            // 不抛异常，避免影响上传主流程（素材停留 pending，人工兜底）
        }
    }

    @Override
    public void deleteMediaVector(Long mediaId) {
        try {
            pythonRestClient.delete()
                    .uri("/api/vector/media_{mediaId}", mediaId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 素材向量删除任务已分发: mediaId={}", mediaId);

        } catch (Exception ex) {
            log.error("HTTP 分发素材向量删除失败: mediaId={}", mediaId, ex);
            // 不抛异常，残留 point 由 Java 侧 is_deleted 二次过滤免疫
        }
    }
}
