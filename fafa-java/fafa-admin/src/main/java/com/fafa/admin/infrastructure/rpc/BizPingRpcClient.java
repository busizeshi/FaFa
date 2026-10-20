package com.fafa.admin.infrastructure.rpc;

import com.fafa.common.rpc.BizPingResult;
import com.fafa.common.rpc.BizPingRpc;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

/** Dubbo's reference annotation is field-based; keep that framework detail behind this adapter. */
@Component
public class BizPingRpcClient {
    @DubboReference(check = false, lazy = true, timeout = 2000)
    private BizPingRpc bizPingRpc;

    public BizPingResult ping() {
        try {
            return bizPingRpc.ping();
        } catch (RpcException exception) {
            throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE,
                    CommonErrorCode.DEPENDENCY_UNAVAILABLE.message(), exception);
        }
    }
}
