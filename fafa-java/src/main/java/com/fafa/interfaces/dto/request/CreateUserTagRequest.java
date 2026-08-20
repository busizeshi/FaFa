package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户标签请求
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Data
@Schema(description = "创建用户标签请求")
public class CreateUserTagRequest {

    @Schema(description = "标签名称", example = "阿酷")
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 20, message = "标签名称不能超过20个字符")
    private String tagName;

    @Schema(description = "标签分类", example = "pet", allowableValues = {"pet", "location", "activity", "mood", "other"})
    @Size(max = 20, message = "标签分类不能超过20个字符")
    private String category;
}
