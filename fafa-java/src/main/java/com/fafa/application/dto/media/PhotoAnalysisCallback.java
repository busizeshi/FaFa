package com.fafa.application.dto.media;

import java.math.BigDecimal;
import java.util.List;

/**
 * 素材 embedding 向量化回调命令（Python → Java）
 *
 * @param mediaId           素材ID
 * @param success           分析是否成功
 * @param recognizedPetIds  达到阈值的全部宠物 ID（可多选，空=未达阈值）
 * @param recognitionScore  识别相似度（成功时，可空）
 * @param errorMessage      失败原因（失败时）
 * @author FaFa Team
 * @since 4.0
 */
public record PhotoAnalysisCallback(
    Long mediaId,
    boolean success,
    List<Long> recognizedPetIds,
    BigDecimal recognitionScore,
    String errorMessage
) {
}
