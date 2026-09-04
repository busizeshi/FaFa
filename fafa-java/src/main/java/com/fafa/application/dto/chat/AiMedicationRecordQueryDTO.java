package com.fafa.application.dto.chat;

import com.fafa.application.dto.medical.MedicationRecordDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 实际用药记录查询结果。 */
@Schema(description = "AI 实际用药记录查询结果")
public record AiMedicationRecordQueryDTO(
        @Schema(description = "实际用药记录列表") List<MedicationRecordDTO> records) {
}
