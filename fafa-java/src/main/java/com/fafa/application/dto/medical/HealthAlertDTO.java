package com.fafa.application.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "健康预警")
public record HealthAlertDTO(
                             @Schema(description = "预警ID", example = "1") Long id,
                             @Schema(description = "所属宠物ID", example = "1") Long petId,
                             @Schema(description = "触发预警的规则编码", example = "REFUSAL_STREAK") String ruleCode,
                             @Schema(description = "预警严重程度", example = "medium") String severity,
                             @Schema(description = "预警标题", example = "连续拒食") String title,
                             @Schema(description = "预警详细信息") String message,
                             @Schema(description = "预警状态：open=未处理，acknowledged=已确认，resolved=已解决", example = "open") String status,
                             @Schema(description = "异常观察开始日期", example = "2026-08-18") LocalDate observedFrom,
                             @Schema(description = "异常观察结束日期", example = "2026-08-20") LocalDate observedTo,
                             @Schema(description = "触发预警的证据列表") List<EvidenceDTO> evidence,
                             @Schema(description = "最近一次检测时间", example = "2026-08-20T10:30:00") LocalDateTime lastDetectedAt) {

    @Schema(description = "健康预警证据")
    public record EvidenceDTO(
                              @Schema(description = "证据来源类型", example = "daily_status") String sourceType,
                              @Schema(description = "证据来源记录ID", example = "1001") String sourceId,
                              @Schema(description = "证据对应字段", example = "appetite") String field,
                              @Schema(description = "字段值", example = "refused") String value,
                              @Schema(description = "证据观察日期", example = "2026-08-20") String observedDate) {
    }
}
