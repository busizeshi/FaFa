package com.fafa.interfaces.dto.callback;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 素材分析结果回调请求体
 *
 * 与 fafa-python 约定 snake_case 字段（见技术文档 7.3 契约）。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PhotoAnalysisCallback {

    /** 素材 ID */
    @NotNull(message = "mediaId 不能为空")
    private Long mediaId;

    /** 分析是否成功 */
    @NotNull(message = "success 不能为空")
    private Boolean success;

    /** AI 生成的素材描述 */
    private String description;

    /** 个体识别判定的宠物 ID（未识别为空） */
    private Long recognizedPetId;

    /** AI 补充的标签 */
    private List<String> aiTags;

    /** 识别相似度（0~1） */
    private java.math.BigDecimal recognitionScore;

    /** 分析失败原因 */
    private String errorMessage;
}
