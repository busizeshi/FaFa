package com.fafa.common.rpc;

/** Small connectivity contract proving the Admin-to-Biz Dubbo path. */
public interface BizPingRpc {
    /** Returns a timestamped liveness result from the Biz application.
     * @return service liveness result
     */
    BizPingResult ping();
}
