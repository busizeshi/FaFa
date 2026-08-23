package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.SemanticSearchGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语义检索网关 HTTP 实现（同步调用 Python /api/photos/search）
 *
 * 复用统一配置的 pythonRestClient（内网令牌 + 读超时覆盖嵌入与检索耗时）。
 * 调用异常向上抛出，由调用方负责配额回滚。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSemanticSearchGateway implements SemanticSearchGateway {

    /** 按名称注入 pythonRestClient */
    private final RestClient pythonRestClient;

    @Override
    public List<SearchHit> search(Long userId, String query, Long petId,
                                  LocalDate startDate, LocalDate endDate, int limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("query", query);
        body.put("limit", limit);
        body.put("pet_id", petId);
        body.put("start_date", startDate == null ? null : startDate.toString());
        body.put("end_date", endDate == null ? null : endDate.toString());

        Map<String, Object> response = pythonRestClient.post()
                .uri("/api/photos/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return parseHits(response);
    }

    /**
     * 解析 Python 返回的 results 列表（media_id + score）
     */
    @SuppressWarnings("unchecked")
    private List<SearchHit> parseHits(Map<String, Object> response) {
        List<SearchHit> hits = new ArrayList<>();
        if (response == null) {
            return hits;
        }
        Object results = response.get("results");
        if (!(results instanceof List<?> list)) {
            return hits;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object mediaId = map.get("media_id");
                Object score = map.get("score");
                if (mediaId instanceof Number id && score instanceof Number sc) {
                    hits.add(new SearchHit(id.longValue(), sc.doubleValue()));
                }
            }
        }
        return hits;
    }
}
