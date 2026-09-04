package com.fafa.application.dto.chat;

import com.fafa.application.dto.medical.MedicationRecordDTO;
import com.fafa.application.dto.medical.MedicationPlanDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 用药计划查询结果。 */
@Schema(description = "AI 用药计划查询结果")
public record AiMedicationPlanQueryDTO(
        @Schema(description = "用药计划列表") List<MedicationPlanDTO> records,
        @Schema(description = "实际用药执行记录列表") List<MedicationRecordDTO> executions) {
}
