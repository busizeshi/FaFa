package com.fafa.interfaces.controller;

import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连通性检测接口（免登录），供小程序与运维探活使用
 *
 * @author FaFa Team
 * @since 1.0
 */
@Tag(name = "通用")
@RestController
public class PingController {

    @Operation(summary = "服务探活")
    @GetMapping("/api/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}
