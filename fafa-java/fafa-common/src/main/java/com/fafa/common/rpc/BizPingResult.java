package com.fafa.common.rpc;

import java.io.Serializable;

/** Serializable response for the initial Admin-to-Biz connectivity check.
 *
 * @param serviceName responding service name
 * @param status service status
 * @param checkedAt check time in ISO-8601 format
 */
public record BizPingResult(String serviceName, String status, String checkedAt) implements Serializable {
}
