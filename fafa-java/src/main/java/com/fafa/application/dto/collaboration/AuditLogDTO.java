package com.fafa.application.dto.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "宠物空间访问审计记录")
public record AuditLogDTO(
        @Schema(description = "被访问用户ID", example = "2") Long userId,
        @Schema(description = "执行操作的用户ID", example = "1") Long operatorId,
        @Schema(description = "操作类型", example = "read") String operation,
        @Schema(description = "资源类型", example = "pet_access") String resourceType,
        @Schema(description = "资源标识") String resourceId,
        @Schema(description = "操作结果", example = "success") String result,
        @Schema(description = "失败或补充原因") String reason,
        @Schema(description = "操作发生时间", example = "2026-08-20T15:30:00") LocalDateTime createdAt) {
}
