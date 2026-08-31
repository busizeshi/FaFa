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

    /** 达到相似度阈值的全部宠物 ID；多个宠物同时命中时全部保留 */
    private List<Long> recognizedPetIds;

    /** 兼容旧版本回调字段；新 embedding 链路不会填充 */
    @Deprecated
    private String description;

    /** 兼容旧版本回调字段；新 embedding 链路不会填充 */
    @Deprecated
    private List<String> aiTags;

    /** 兼容旧版本回调的单个宠物 ID */
    private Long recognizedPetId;

    /** 识别相似度（0~1） */
    private java.math.BigDecimal recognitionScore;

    /** 分析失败原因 */
    private String errorMessage;
}
