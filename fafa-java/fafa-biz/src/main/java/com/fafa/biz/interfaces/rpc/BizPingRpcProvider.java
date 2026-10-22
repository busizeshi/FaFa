package com.fafa.biz.interfaces.rpc;

import com.fafa.common.rpc.BizPingResult;
import com.fafa.common.rpc.BizPingRpc;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.Instant;

/** 初始脚手架使用的连通性服务提供者；后续应逐步替换为真实业务契约。 */
@DubboService
public class BizPingRpcProvider implements BizPingRpc {
    @Override
    public BizPingResult ping() {
        return new BizPingResult("fafa-biz", "UP", Instant.now().toString());
    }
}
