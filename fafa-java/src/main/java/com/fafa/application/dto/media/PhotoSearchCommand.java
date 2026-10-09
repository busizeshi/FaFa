package com.fafa.application.dto.media;

import java.time.LocalDate;
import java.util.List;

/**
 * 素材融合检索命令（确定性 + 语义）
 *
 * query 为空走确定性分页检索（不限次）；query 有值走语义检索（占 AI 月配额），
 * petId/tags/日期作为确定性硬过滤叠加。
 *
 * @author FaFa Team
 * @since 4.0
 */
public record PhotoSearchCommand(
        String query,
        Long petId,
        List<String> tags,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer size
) {
}
