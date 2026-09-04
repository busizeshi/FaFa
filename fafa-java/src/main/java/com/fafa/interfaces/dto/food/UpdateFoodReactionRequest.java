package com.fafa.interfaces.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 更新换粮反应请求。 */
@Data
@Schema(description = "记录每日换粮反应请求")
public class UpdateFoodReactionRequest {
    @NotNull @Schema(description = "反应记录日期", example = "2026-09-03") private LocalDate date;
    @NotBlank @Schema(description = "宠物换粮后的反应描述或编码", example = "normal") private String reaction;
    @Size(max = 500) @Schema(description = "反应补充备注") private String note;
}
