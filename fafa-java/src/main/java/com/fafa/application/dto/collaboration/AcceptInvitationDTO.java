package com.fafa.application.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "接受邀请结果")
public record AcceptInvitationDTO(
        @Schema(description = "宠物ID", example = "1") Long petId,
        @Schema(description = "当前用户获得的角色", example = "caregiver") String role,
        @Schema(description = "成员状态", example = "active") String status,
        @Schema(description = "成员权限到期时间") LocalDateTime expiresAt) {
}
