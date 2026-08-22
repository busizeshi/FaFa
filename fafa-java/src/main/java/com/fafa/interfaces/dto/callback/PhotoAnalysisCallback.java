package com.fafa.interfaces.dto.callback;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 照片分析结果回调请求体
 *
 * 与 fafa-python 约定 snake_case 字段（见技术文档 7.3 契约）。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PhotoAnalysisCallback {

    /** 照片 ID */
    @NotNull(message = "photoId 不能为空")
    private Long photoId;

    /** AI 生成的照片描述 */
    private String description;

    /** 个体识别判定的宠物 ID（未识别为空） */
    private Long recognizedPetId;

    /** AI 补充的标签 */
    private List<String> tags;

    /** 幂等键 */
    private String messageId;
}
