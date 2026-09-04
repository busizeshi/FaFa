package com.fafa.application.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "宠物空间邀请")
public record InvitationDTO(
                            @Schema(description = "邀请ID", example = "1") Long invitationId,
                            @Schema(description = "宠物ID", example = "1") Long petId,
                            @Schema(description = "邀请角色", example = "caregiver") String role,
                            @Schema(description = "一次性邀请凭证") String token,
                            @Schema(description = "邀请有效期截止时间") LocalDateTime expiresAt,
                            @Schema(description = "成员权限有效期截止时间") LocalDateTime memberExpiresAt,
                            @Schema(description = "邀请分享路径") String sharePath) {
}
