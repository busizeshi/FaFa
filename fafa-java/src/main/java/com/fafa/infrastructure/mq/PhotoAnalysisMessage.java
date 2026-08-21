package com.fafa.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 照片/视频分析消息
 *
 * Java -> Python 的异步 AI 任务消息体。
 * 字段 camelCase（与 Java 序列化一致），Python 消费端做 camelCase -> snake_case 映射。
 * 契约变更必须同步 fafa-python 消费端与本表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAnalysisMessage implements Serializable {

    /** 幂等键 */
    private String messageId;

    /** 全链路追踪 ID */
    private String traceId;

    private Long photoId;

    private Long userId;

    /** 归属宠物，可为空（新模型下素材可不挂宠物） */
    private Long petId;

    /** 素材访问 URL */
    private String url;

    /** media type: photo / video */
    private String mediaType;

    /** 用户选择的标签 */
    private List<String> tags;
}
