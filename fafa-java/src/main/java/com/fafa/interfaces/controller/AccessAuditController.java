package com.fafa.interfaces.controller;

import com.fafa.application.dto.collaboration.AuditLogDTO;
import com.fafa.application.service.AccessAuditApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "访问审计", description = "宠物主人查看成员操作审计")
public class AccessAuditController {
    private final AccessAuditApplicationService auditService;

    @GetMapping
    @Operation(summary = "查询宠物空间审计记录")
    public Result<List<AuditLogDTO>> list(
                          @Parameter(description = "宠物ID", required = true, example = "1") @PathVariable Long petId,
                          @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
                          @Parameter(description = "每页记录数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(auditService.list(petId, page, size));
    }
}
