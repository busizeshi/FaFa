package com.fafa.interfaces.dto.food;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 拒绝食品识别草稿请求。 */
@Data
public class RejectFoodRecognitionRequest {
    @Size(max = 200)
    private String reason;
}
