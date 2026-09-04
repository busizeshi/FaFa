package com.fafa.application.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "宠物空间成员")
public record PetMemberDTO(
                           @Schema(description = "成员关系ID", example = "1") Long memberId,
                           @Schema(description = "成员用户ID", example = "2") Long userId,
                           @Schema(description = "成员昵称", example = "张三") String nickname,
                           @Schema(description = "成员角色", example = "caregiver") String role,
                           @Schema(description = "成员状态", example = "active") String status,
                           @Schema(description = "加入时间", example = "2026-08-20T10:30:00") LocalDateTime joinedAt,
                           @Schema(description = "成员权限到期时间") LocalDateTime expiresAt) {
}
