package com.fafa.admin.interfaces.http;

import com.fafa.admin.infrastructure.rpc.BizPingRpcClient;
import com.fafa.common.api.ApiResponse;
import com.fafa.common.rpc.BizPingResult;
import com.fafa.common.web.RequestIdContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin")
public class SystemController {
    private final BizPingRpcClient bizPingRpcClient;

    public SystemController(BizPingRpcClient bizPingRpcClient) {
        this.bizPingRpcClient = bizPingRpcClient;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping(@RequestHeader(RequestIdContext.HEADER_NAME) String requestId) {
        return ApiResponse.success("fafa-admin is UP", requestId);
    }

    @GetMapping("/biz-ping")
    public ApiResponse<BizPingResult> pingBiz(@RequestHeader(RequestIdContext.HEADER_NAME) String requestId) {
        return ApiResponse.success(bizPingRpcClient.ping(), requestId);
    }
}
