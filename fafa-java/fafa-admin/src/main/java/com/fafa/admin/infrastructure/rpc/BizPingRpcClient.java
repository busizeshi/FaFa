package com.fafa.admin.infrastructure.rpc;

import com.fafa.common.rpc.BizPingResult;
import com.fafa.common.rpc.BizPingRpc;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

/** Dubbo 的引用注解基于字段注入；将该框架细节封装在此适配器之后。 */
@Component
@org.springframework.context.annotation.Profile("!admin-init")
public class BizPingRpcClient {
    @DubboReference(check = false, lazy = true, timeout = 2000, url = "${fafa.biz-rpc.url:}")
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
