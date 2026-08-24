package com.fafa.interfaces.dto.food;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建食品包装识别任务请求。 */
@Data
public class CreateFoodRecognitionRequest {
    @NotNull
    private Long petId;
    @NotNull
    private Long mediaId;
    @Size(max = 100)
    private String idempotencyKey;
}
