package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.quota.QuotaDTO;
import com.fafa.application.service.QuotaApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配额接口控制器
 *
 * @author FaFa Team
 * @since 4.0
 */
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
@Tag(name = "配额", description = "存储、AI 检索和食品包装识别配额用量查询")
public class QuotaController {

    private final QuotaApplicationService quotaApplicationService;

    @GetMapping
    @Operation(summary = "配额与用量", description = "存储、AI 检索、食品包装识别用量/上限 + 会员状态")
    public Result<QuotaDTO> getQuota() {
        return Result.ok(quotaApplicationService.getQuota(StpUtil.getLoginIdAsLong()));
    }
}
