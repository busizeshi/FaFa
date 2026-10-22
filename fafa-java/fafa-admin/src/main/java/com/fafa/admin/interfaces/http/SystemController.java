package com.fafa.admin.interfaces.http;

import com.fafa.admin.infrastructure.rpc.BizPingRpcClient;
import com.fafa.common.api.R;
import com.fafa.common.rpc.BizPingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.context.annotation.Profile("!admin-init")
@RequestMapping("/internal/admin")
public class SystemController {
    private final BizPingRpcClient bizPingRpcClient;

    public SystemController(BizPingRpcClient bizPingRpcClient) {
        this.bizPingRpcClient = bizPingRpcClient;
    }

    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("fafa-admin is UP");
    }

    @GetMapping("/biz-ping")
    public R<BizPingResult> pingBiz() {
        return R.ok(bizPingRpcClient.ping());
    }
}
