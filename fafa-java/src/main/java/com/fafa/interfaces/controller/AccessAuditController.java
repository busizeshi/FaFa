package com.fafa.interfaces.controller;

import com.fafa.application.service.AccessAuditApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets/{petId}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "访问审计", description = "宠物主人查看成员操作审计")
public class AccessAuditController {
    private final AccessAuditApplicationService auditService;

    @GetMapping
    @Operation(summary = "查询宠物空间审计记录")
    public Result<?> list(@PathVariable Long petId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return Result.ok(auditService.list(petId, page, size));
    }
}
