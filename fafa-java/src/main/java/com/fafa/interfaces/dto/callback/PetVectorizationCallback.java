package com.fafa.interfaces.dto.callback;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 宠物三视图向量化回调
 * <p>
 * Python 完成向量化（含写入 Qdrant）后回调本接口，snake_case 契约：pet_id / success / message。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "宠物三视图向量化回调")
public class PetVectorizationCallback {

    @NotNull(message = "petId 不能为空")
    @Schema(description = "宠物ID", example = "123")
    private Long petId;

    @NotNull(message = "success 不能为空")
    @Schema(description = "向量化是否成功", example = "true")
    private Boolean success;

    @Schema(description = "失败原因等附加信息")
    private String message;
}
