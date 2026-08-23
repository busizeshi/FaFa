package com.fafa.interfaces.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保存每日状态请求（upsert 语义，同日重复提交覆盖）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@Schema(description = "保存每日状态请求")
public class SaveDailyStatusRequest {

    @NotNull(message = "宠物ID不能为空")
    @Schema(description = "宠物ID", example = "1")
    private Long petId;

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期（一天一格）", example = "2026-08-22")
    private LocalDate recordDate;

    @Schema(description = "食欲: excellent/normal/decreased/refused", example = "normal")
    private String appetite;

    @Schema(description = "精神活动: active/normal/lethargic", example = "active")
    private String energy;

    @Schema(description = "饮水程度: normal/more/less/not_noticed", example = "normal")
    private String waterIntake;

    @Schema(description = "排便: normal/soft/diarrhea/constipation/not_seen", example = "normal")
    private String defecation;

    @Schema(description = "呕吐: none/once/multiple", example = "none")
    private String vomiting;

    @Schema(description = "排尿: normal/abnormal/not_seen", example = "normal")
    private String urination;

    @DecimalMin(value = "0", message = "进食克数不能为负")
    @Schema(description = "进食克数（选填）", example = "80")
    private BigDecimal appetiteAmount;

    @DecimalMin(value = "0", message = "饮水毫升数不能为负")
    @Schema(description = "饮水毫升数（选填）", example = "200")
    private BigDecimal waterAmount;

    @Size(max = 500, message = "备注不能超过500字符")
    @Schema(description = "备注", example = "换新粮第一天")
    private String notes;
}
