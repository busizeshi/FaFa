package com.fafa.application.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "当前用户宠物空间访问权限")
public record PetAccessDTO(
                           @Schema(description = "宠物ID", example = "1") Long petId,
                           @Schema(description = "当前用户角色", example = "owner") String role,
                           @Schema(description = "成员状态", example = "active") String status,
                           @Schema(description = "成员权限到期时间") LocalDateTime expiresAt,
                           @Schema(description = "权限编码集合", example = "[\"pet:read\", \"pet:write\"]") Set<String> permissions,
                           @Schema(description = "服务端当前时间") LocalDateTime serverTime) {
}
