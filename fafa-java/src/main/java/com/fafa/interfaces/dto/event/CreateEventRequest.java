package com.fafa.interfaces.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 手动创建事件请求。 */
@Data
@Schema(description = "手动创建成长档案事件请求")
public class CreateEventRequest {

    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    @NotBlank(message = "事件类型不能为空")
    @Schema(description = "事件类型: vaccine/deworming/bath/nail_trim/checkup/custom")
    private String eventType;

    @NotBlank(message = "事件标题不能为空")
    @Size(max = 200, message = "事件标题不能超过200字符")
    private String title;

    @Size(max = 2000, message = "事件内容不能超过2000字符")
    private String content;

    @NotNull(message = "事件日期不能为空")
    private LocalDate eventDate;

    @Size(max = 9, message = "事件素材不能超过9个")
    private List<String> materialUrls;

    @Size(max = 10, message = "事件标签不能超过10个")
    private List<String> tags;
}
