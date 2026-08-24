package com.fafa.interfaces.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "创建宠物空间邀请请求")
public class CreateInvitationRequest {
    @NotBlank(message = "邀请角色不能为空")
    @Schema(description = "member/caregiver", example = "caregiver")
    private String role;

    @Min(value = 1, message = "邀请有效天数必须大于0")
    @Schema(description = "成员权限有效天数；member必须为null或忽略", example = "7")
    private Integer expiresInDays;
}
