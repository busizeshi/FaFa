package com.fafa.biz.interfaces.rpc;

import com.fafa.common.rpc.BizPingResult;
import com.fafa.common.rpc.BizPingRpc;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.Instant;

/** Connectivity provider used by the initial scaffold; replace with real application contracts over time. */
@DubboService
public class BizPingRpcProvider implements BizPingRpc {
    @Override
    public BizPingResult ping() {
        return new BizPingResult("fafa-biz", "UP", Instant.now().toString());
    }
}
