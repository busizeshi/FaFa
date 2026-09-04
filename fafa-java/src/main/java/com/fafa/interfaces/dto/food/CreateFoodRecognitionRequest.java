package com.fafa.interfaces.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建食品包装识别任务请求。 */
@Data
@Schema(description = "创建食品包装识别任务请求")
public class CreateFoodRecognitionRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotNull
    @Schema(description = "待识别的食品包装图片媒体ID", example = "1001")
    private Long mediaId;
    @Size(max = 100)
    @Schema(description = "幂等键；相同用户和幂等键不会重复创建任务", example = "food-recognition-20260901-001")
    private String idempotencyKey;
}
