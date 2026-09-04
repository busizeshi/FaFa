package com.fafa.interfaces.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 拒绝食品识别草稿请求。 */
@Data
@Schema(description = "拒绝食品识别草稿请求")
public class RejectFoodRecognitionRequest {
    @Size(max = 200)
    @Schema(description = "拒绝原因", example = "包装图片不清晰")
    private String reason;
}
