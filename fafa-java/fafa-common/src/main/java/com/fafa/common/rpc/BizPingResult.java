package com.fafa.common.rpc;

import java.io.Serializable;

/** 初始 Admin 到 Biz 连通性检查使用的可序列化响应。
 *
 * @param serviceName 响应服务名称
 * @param status 服务状态
 * @param checkedAt ISO-8601 格式的检查时间
 */
public record BizPingResult(String serviceName, String status, String checkedAt) implements Serializable {
}
