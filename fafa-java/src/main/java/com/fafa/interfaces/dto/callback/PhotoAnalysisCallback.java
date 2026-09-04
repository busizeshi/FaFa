package com.fafa.interfaces.dto.callback;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "素材分析结果回调请求体")
public class PhotoAnalysisCallback {

    /** 素材 ID */
    @NotNull(message = "mediaId 不能为空")
    @Schema(description = "待回写分析结果的素材ID", example = "1001")
    private Long mediaId;

    /** 分析是否成功 */
    @NotNull(message = "success 不能为空")
    @Schema(description = "素材分析是否成功", example = "true")
    private Boolean success;

    /** 达到相似度阈值的全部宠物 ID；多个宠物同时命中时全部保留 */
    @Schema(description = "达到相似度阈值的全部宠物ID；多个宠物命中时全部保留", example = "[1, 2]")
    private List<Long> recognizedPetIds;

    /** 兼容旧版本回调字段；新 embedding 链路不会填充 */
    @Deprecated
    @Schema(description = "兼容旧版本回调的描述字段，新 embedding 链路不会填充")
    private String description;

    /** 兼容旧版本回调字段；新 embedding 链路不会填充 */
    @Deprecated
    @Schema(description = "兼容旧版本回调的 AI 标签，新 embedding 链路不会填充")
    private List<String> aiTags;

    /** 兼容旧版本回调的单个宠物 ID */
    @Schema(description = "兼容旧版本回调的单个宠物ID")
    private Long recognizedPetId;

    /** 识别相似度（0~1） */
    @Schema(description = "识别相似度，范围 0-1", example = "0.95")
    private java.math.BigDecimal recognitionScore;

    /** 分析失败原因 */
    @Schema(description = "分析失败原因，成功时通常为空")
    private String errorMessage;
}
