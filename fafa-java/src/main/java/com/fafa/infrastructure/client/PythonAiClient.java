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
import java.util.List;
import java.util.Map;

/**
 * Python AI 服务客户端
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
     * 照片语义搜索
     */
    @SuppressWarnings("unchecked")
    public List<PhotoSearchResult> searchPhotos(SearchPhotoRequest request) {
        try {
            String url = aiServiceBaseUrl + "/ai/photos/search";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<SearchPhotoRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                
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
     * 转换搜索结果
     */
    private PhotoSearchResult convertToPhotoSearchResult(Map<String, Object> data) {
        return PhotoSearchResult.builder()
                .photoId(((Number) data.get("photo_id")).longValue())
                .petId(((Number) data.get("pet_id")).longValue())
                .url((String) data.get("url"))
                .thumbnailUrl((String) data.get("thumbnail_url"))
                .description((String) data.get("description"))
                .aiDescription((String) data.get("ai_description"))
                .score(((Number) data.get("score")).doubleValue())
                .build();
    }
}
