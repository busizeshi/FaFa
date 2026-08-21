package com.fafa.infrastructure.client;

import com.fafa.application.dto.photo.PhotoSearchResult;
import com.fafa.application.dto.photo.SearchPhotoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python AI 服务客户端
 *
 * 服务间契约:
 * 1. Python 为内部无鉴权服务，所有请求显式携带 user_id（鉴权已在 Java 网关完成）
 * 2. 请求/响应字段统一使用 snake_case
 * 3. 调用失败一律降级（返回空列表/false），不阻断 Java 主流程
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Slf4j
@Service
public class PythonAiClient {

    @Resource
    private RestTemplate restTemplate;

    @Value("${fafa.ai-service.base-url}")
    private String aiServiceBaseUrl;

    /**
     * 照片/视频语义搜索（跨模态）
     */
    @SuppressWarnings("unchecked")
    public List<PhotoSearchResult> searchPhotos(Long userId, SearchPhotoRequest request) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("query", request.getQuery());
            body.put("pet_id", request.getPetId());
            body.put("limit", request.getLimit());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceBaseUrl + "/api/photos/search",
                    new HttpEntity<>(body, jsonHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                if (results == null) {
                    return Collections.emptyList();
                }
                return results.stream()
                        .map(this::convertToPhotoSearchResult)
                        .toList();
            }

            log.warn("照片搜索请求失败，返回空列表");
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("调用 Python AI 服务搜索照片失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 上传宠物三视图到 AI 服务生成识别向量（用于照片自动识别宠物）
     */
    public void uploadPetProfilePhotos(Long petId, Long userId,
                                       String frontViewUrl, String sideViewUrl, String topViewUrl) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pet_id", petId);
            body.put("user_id", userId);
            body.put("front_view_url", frontViewUrl);
            body.put("side_view_url", sideViewUrl);
            body.put("top_view_url", topViewUrl);

            restTemplate.postForEntity(
                    aiServiceBaseUrl + "/api/pets/profile-photos",
                    new HttpEntity<>(body, jsonHeaders()),
                    Map.class
            );

        } catch (Exception e) {
            log.error("调用 AI 服务上传宠物三视图失败: petId={}, {}", petId, e.getMessage());
        }
    }

    /**
     * 删除宠物三视图向量（宠物删除时同步清理）
     */
    public void deletePetProfileVectors(Long petId) {
        try {
            restTemplate.delete(aiServiceBaseUrl + "/api/pets/" + petId + "/profile-vectors");

        } catch (Exception e) {
            log.error("调用 AI 服务删除宠物三视图向量失败: petId={}, {}", petId, e.getMessage());
        }
    }

    /**
     * 删除媒体向量（照片/视频删除时同步清理）
     */
    public void deleteMediaVector(String embeddingId) {
        if (embeddingId == null || embeddingId.isBlank()) {
            return;
        }
        try {
            restTemplate.delete(aiServiceBaseUrl + "/api/vector/" + embeddingId);

        } catch (Exception e) {
            log.error("调用 AI 服务删除媒体向量失败: embeddingId={}, {}", embeddingId, e.getMessage());
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 转换搜索结果（pet_id 可能为 null——未识别到宠物，做 null 安全处理）
     */
    @SuppressWarnings("unchecked")
    private PhotoSearchResult convertToPhotoSearchResult(Map<String, Object> data) {
        Number petId = (Number) data.get("pet_id");
        Number score = (Number) data.get("score");
        return PhotoSearchResult.builder()
                .photoId(((Number) data.get("photo_id")).longValue())
                .petId(petId != null ? petId.longValue() : null)
                .url((String) data.get("url"))
                .mediaType((String) data.get("media_type"))
                .tags((List<String>) data.get("tags"))
                .score(score != null ? score.doubleValue() : null)
                .build();
    }
}
