package com.fafa.application.dto.chat;

import com.fafa.application.dto.medical.HealthAlertDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 健康预警查询结果。 */
@Schema(description = "AI 健康预警查询结果")
public record AiHealthAlertQueryDTO(
        @Schema(description = "健康预警列表") List<HealthAlertDTO> records) {
}
