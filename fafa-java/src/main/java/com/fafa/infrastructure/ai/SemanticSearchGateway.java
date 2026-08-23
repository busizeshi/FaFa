package com.fafa.infrastructure.ai;

import java.time.LocalDate;
import java.util.List;

/**
 * 语义检索网关（Java → Python /api/photos/search）
 *
 * 同步 HTTP 调用：文本嵌入 → Qdrant fafa_media 检索 → 返回 mediaId + score。
 * 调用异常由调用方负责配额回滚（失败请求不占额度）。
 *
 * @author FaFa Team
 * @since 4.0
 */
public interface SemanticSearchGateway {

    /**
     * 语义检索
     *
     * @param userId    用户ID（Qdrant filter 必带）
     * @param query     自然语言查询
     * @param petId     限定宠物，可空
     * @param startDate 拍摄日期起，可空
     * @param endDate   拍摄日期止，可空
     * @param limit     返回数量（1~100）
     * @return 命中结果（mediaId + score，按相关度降序）
     */
    List<SearchHit> search(Long userId, String query, Long petId,
                           LocalDate startDate, LocalDate endDate, int limit);

    /**
     * 检索命中项
     */
    record SearchHit(Long mediaId, double score) {
    }
}
