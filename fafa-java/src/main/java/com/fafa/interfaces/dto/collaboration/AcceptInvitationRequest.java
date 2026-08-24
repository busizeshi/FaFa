package com.fafa.interfaces.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "接受宠物空间邀请请求")
public class AcceptInvitationRequest {
    @NotNull(message = "邀请ID不能为空")
    private Long invitationId;

    @NotBlank(message = "邀请凭证不能为空")
    @Schema(description = "一次性邀请凭证，只提交服务端返回的原始token", example = "raw-token")
    private String token;
}
