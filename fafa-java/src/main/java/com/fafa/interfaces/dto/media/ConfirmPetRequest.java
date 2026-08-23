package com.fafa.interfaces.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 确认/修正素材归属请求
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@Schema(description = "确认/修正素材归属请求")
public class ConfirmPetRequest {

    @Schema(description = "归属宠物ID；传 null 表示清除归属（等待重新识别）", example = "1")
    private Long petId;
}
