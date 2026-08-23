package com.fafa.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON 数组列与 List&lt;String&gt; 互转
 *
 * 用于 photo_urls/material_urls/tags 等 JSON 列，领域对象保持 List&lt;String&gt;。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Component
@RequiredArgsConstructor
public class JsonListConverter {

    private final ObjectMapper objectMapper;

    /**
     * List 转 JSON 文本（空列表存 null）
     */
    public String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    /**
     * JSON 文本转 List（空文本返回 null）
     */
    public List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("JSON 反序列化失败", e);
        }
    }
}
