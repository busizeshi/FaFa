package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日状态记录DTO
 *
 * abnormalDimensions 为异常维度 code 列表，前端高亮与预警展示用。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Schema(description = "每日状态记录")
public record DailyStatusDTO(
    @Schema(description = "记录ID", example = "1") Long id,
    @Schema(description = "所属宠物ID", example = "1") Long petId,
    @Schema(description = "记录日期", example = "2026-08-20") LocalDate recordDate,
    @Schema(description = "食欲：excellent=很好，normal=正常，decreased=下降，refused=拒食", example = "normal") String appetite,
    @Schema(description = "精神活动：active=活跃，normal=正常，lethargic=无精打采", example = "active") String energy,
    @Schema(description = "饮水情况：normal=正常，more=增多，less=减少，not_noticed=未留意", example = "normal") String waterIntake,
    @Schema(description = "排便情况：normal=正常，soft=偏软，diarrhea=腹泻，constipation=便秘，not_seen=未观察到", example = "normal") String defecation,
    @Schema(description = "呕吐情况：none=无，once=一次，multiple=多次", example = "none") String vomiting,
    @Schema(description = "排尿情况：normal=正常，abnormal=异常，not_seen=未观察到", example = "normal") String urination,
    @Schema(description = "进食量（克）", example = "80") BigDecimal appetiteAmount,
    @Schema(description = "饮水量（毫升）", example = "200") BigDecimal waterAmount,
    @Schema(description = "备注") String notes,
    @Schema(description = "是否所有状态均正常", example = "true") Boolean allNormal,
    @Schema(description = "异常维度编码列表") List<String> abnormalDimensions,
    @Schema(description = "创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
    @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt
) {
}
