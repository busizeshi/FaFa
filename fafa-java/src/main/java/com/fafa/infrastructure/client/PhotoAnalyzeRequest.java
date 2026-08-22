package com.fafa.infrastructure.client;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 照片分析请求（Java → Python，契约见技术文档 7.3）
 *
 * 字段 snake_case，与 fafa-python 侧 Pydantic 模型逐字节一致。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PhotoAnalyzeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "photoId 不能为空")
    private Long photoId;

    @NotBlank(message = "url 不能为空")
    private String url;

    /** photo / video / pet_profile */
    @NotBlank(message = "mediaType 不能为空")
    private String mediaType;

    private List<String> tags;

    /** 已判定的宠物 ID，可空 */
    private Long petId;

    /** 幂等键 */
    private String messageId;
}
