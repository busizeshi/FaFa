package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 同步调用 AI 任务分发实现（开发环境）
 *
 * 照片已上传到对象存储，仅传 URL 到 Python 服务，
 * 由 Python 调用 qwen3-vl-embedding 生成向量并写入 Qdrant。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.ai.dispatcher", havingValue = "http", matchIfMissing = true)
public class HttpAiTaskDispatcher implements AiTaskDispatcher {

    private final RestTemplate restTemplate;
    private final FaFaProperties properties;

    @Override
    public void dispatchPetPhotoVectorization(Long petId, Long userId,
                                              String avatarUrl, String frontPhotoUrl, String sidePhotoUrl) {
        try {
            String url = properties.getPython().getBaseUrl() + "/api/pets/profile-photos";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", properties.getInternalToken());

            Map<String, Object> body = new HashMap<>();
            body.put("pet_id", petId);
            body.put("user_id", userId);
            body.put("avatar_url", avatarUrl != null ? avatarUrl : "");
            body.put("front_url", frontPhotoUrl != null ? frontPhotoUrl : "");
            body.put("side_url", sidePhotoUrl != null ? sidePhotoUrl : "");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, requestEntity, String.class);

            log.info("HTTP 宠物照片向量化任务已分发: petId={}, userId={}", petId, userId);

        } catch (Exception ex) {
            log.error("HTTP 分发宠物照片向量化失败: petId={}, userId={}", petId, userId, ex);
            // 不抛异常，避免影响主流程（照片已上传成功，向量化可重试）
        }
    }
}
