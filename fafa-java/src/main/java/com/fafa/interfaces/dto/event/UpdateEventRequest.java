package com.fafa.interfaces.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 手动编辑事件请求。 */
@Data
@Schema(description = "手动编辑成长档案事件请求")
public class UpdateEventRequest {

    @NotBlank(message = "事件类型不能为空")
    @Schema(description = "事件类型：vaccine=疫苗，deworming=驱虫，bath=洗澡，nail_trim=剪指甲，checkup=体检，custom=自定义", example = "checkup")
    private String eventType;

    @NotBlank(message = "事件标题不能为空")
    @Size(max = 200, message = "事件标题不能超过200字符")
    @Schema(description = "事件标题", example = "年度体检")
    private String title;

    @Size(max = 2000, message = "事件内容不能超过2000字符")
    @Schema(description = "事件详细内容")
    private String content;

    @NotNull(message = "事件日期不能为空")
    @Schema(description = "事件发生日期", example = "2026-08-20")
    private LocalDate eventDate;

    @Size(max = 9, message = "事件素材不能超过9个")
    @Schema(description = "关联素材URL列表，最多9个")
    private List<String> materialUrls;

    @Size(max = 10, message = "事件标签不能超过10个")
    @Schema(description = "事件标签列表，最多10个")
    private List<String> tags;
}
