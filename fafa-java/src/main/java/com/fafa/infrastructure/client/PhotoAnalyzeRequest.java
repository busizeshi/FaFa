package com.fafa.infrastructure.client;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.time.LocalDateTime;

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

    /** 对象存储永久引用，写入 Qdrant；url 可为视频临时预签名地址 */
    private String fileUrl;

    /** photo / video / pet_profile */
    @NotBlank(message = "mediaType 不能为空")
    private String mediaType;

    private List<String> tags;

    /** 已判定的宠物 ID，可空 */
    private Long petId;

    /** 多宠物匹配候选 ID；单宠物/指定宠物为空 */
    private List<Long> candidatePetIds;

    /** 素材拍摄时间，用于语义检索时间过滤 */
    private LocalDateTime takenAt;

    /** 素材上传入库时间，写入 Qdrant 用于追溯 */
    private LocalDateTime uploadedAt;

    /** 幂等键 */
    private String messageId;
}
