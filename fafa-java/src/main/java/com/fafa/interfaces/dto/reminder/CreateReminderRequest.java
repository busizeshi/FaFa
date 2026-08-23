package com.fafa.interfaces.dto.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建提醒请求
 *
 * @author FaFa Team
 * @since 3.0
 */
@Data
@Schema(description = "创建提醒请求")
public class CreateReminderRequest {

    @NotNull(message = "宠物ID不能为空")
    @Schema(description = "宠物ID", example = "1")
    private Long petId;

    @NotBlank(message = "提醒类型不能为空")
    @Schema(description = "提醒类型: vaccine/deworming/bath/nail_trim/checkup/medication/birthday/daily_record/custom", example = "vaccine")
    private String type;

    @NotBlank(message = "提醒标题不能为空")
    @Size(max = 200, message = "提醒标题不能超过200字符")
    @Schema(description = "提醒标题", example = "接种狂犬疫苗")
    private String title;

    @Size(max = 500, message = "提醒描述不能超过500字符")
    @Schema(description = "提醒描述", example = "第三针狂犬疫苗")
    private String description;

    @NotBlank(message = "周期类型不能为空")
    @Schema(description = "周期类型: once/days/weekly/monthly/months/yearly", example = "yearly")
    private String intervalType;

    @Schema(description = "周期值: days/months=N(≥1); weekly=ISO星期(1周一~7周日); monthly=每月几号(1-31); once/yearly不使用", example = "null")
    private Integer intervalValue;

    @NotNull(message = "下次提醒时间不能为空")
    @Schema(description = "下次提醒时间", example = "2026-09-01T09:00:00")
    private LocalDateTime nextRemindTime;
}
