package com.fafa.application.dto.media;

import java.math.BigDecimal;
import java.util.List;

/**
 * 素材 AI 分析回调命令（Python → Java）
 *
 * @param mediaId           素材ID
 * @param success           分析是否成功
 * @param description       AI 视觉理解描述（成功时）
 * @param aiTags            AI 生成标签（成功时）
 * @param recognizedPetId   识别出的归属宠物（成功时，可空=未达阈值）
 * @param recognitionScore  识别相似度（成功时，可空）
 * @param errorMessage      失败原因（失败时）
 * @author FaFa Team
 * @since 4.0
 */
public record PhotoAnalysisCallback(
    Long mediaId,
    boolean success,
    String description,
    List<String> aiTags,
    Long recognizedPetId,
    BigDecimal recognitionScore,
    String errorMessage
) {
}
