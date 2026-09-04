package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "关联就医素材请求")
public class AttachMedicalMediaRequest {
    @NotEmpty
    @Size(max = 9)
    @Schema(description = "待关联的素材ID列表，最多9个", example = "[1001, 1002]")
    private List<Long> mediaIds;
    @Schema(description = "素材关联类型", example = "report")
    private String attachmentType = "other";
}
