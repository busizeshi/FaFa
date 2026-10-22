package com.fafa.common.rpc;

/** 用于验证 Admin 到 Biz 的 Dubbo 通信链路的小型连通性契约。 */
public interface BizPingRpc {
    /** 返回 Biz 应用带时间戳的存活结果。
     * @return 服务存活结果
     */
    BizPingResult ping();
}
