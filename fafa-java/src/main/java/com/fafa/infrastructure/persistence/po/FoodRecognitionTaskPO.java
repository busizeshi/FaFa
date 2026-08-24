package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 食品识别任务持久化对象。 */
@Data
@TableName("food_recognition_task")
public class FoodRecognitionTaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private Long mediaId;
    private String idempotencyKey;
    private String status;
    private String modelName;
    private String promptVersion;
    private String draftJson;
    private String rawResponseJson;
    private String errorCode;
    private String errorMessage;
    private Long foodCardId;
    private String quotaMonth;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
