package com.fafa.application.dto.chat;

import com.fafa.application.dto.medical.MedicalVisitDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 就医档案查询结果。 */
@Schema(description = "AI 就医档案查询结果")
public record AiMedicalVisitQueryDTO(
        @Schema(description = "就医记录列表") List<MedicalVisitDTO> records) {
}
