package com.fafa.application.dto.medical;

import com.fafa.application.dto.media.MediaDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "就医档案")
public record MedicalVisitDTO(
                              @Schema(description = "就医档案ID", example = "1") Long id,
                              @Schema(description = "所属宠物ID", example = "1") Long petId,
                              @Schema(description = "就诊日期", example = "2026-08-20") LocalDate visitDate,
                              @Schema(description = "就诊医院名称", example = "爱宠动物医院") String hospitalName,
                              @Schema(description = "接诊医生姓名", example = "李医生") String doctorName,
                              @Schema(description = "主诉，即本次就诊主要问题") String chiefComplaint,
                              @Schema(description = "诊断结果") String diagnosis,
                              @Schema(description = "处方内容") String prescription,
                              @Schema(description = "治疗或处置内容") String treatment,
                              @Schema(description = "本次就诊总费用", example = "320.00") BigDecimal totalCost,
                              @Schema(description = "就诊备注") String notes,
                              @Schema(description = "关联医疗素材列表") List<MediaDTO> attachments,
                              @Schema(description = "创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
                              @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt) {
}
