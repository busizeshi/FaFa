package com.fafa.interfaces.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@Schema(description = "创建标签请求")
public class CreateTagRequest {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 50, message = "标签名不能超过50字符")
    @Schema(description = "标签名", example = "晒太阳")
    private String name;

    @Schema(description = "分类（可选）: pet/location/action/object/mood/other", example = "action")
    private String category;
}
