package com.fafa.common.constant;

/**
 * 跨服务共享的 HTTP 协议头常量。
 *
 * <p>说明：所有请求头名称一经发布即为契约，改名必须走接口契约变更流程。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public final class HeaderConstants {

    /** 请求关联 ID：由网关生成（或沿用客户端传入），贯穿网关→Java 服务→Python 的日志链路 */
    public static final String REQUEST_ID = "X-Request-Id";

    /** 链路追踪 ID：由 OpenTelemetry 自动注入（W3C traceparent），此处仅供日志检索命名对齐 */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 幂等键：所有可能因断网重试的写操作必须携带（架构规范 §9.1） */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /**
     * ⚠️ 伪造身份头黑名单：客户端自带的以下请求头一律在网关剥离（架构规范 §4.1），
     * 身份只能来自服务端会话（Sa-Token），绝不信任客户端声明。
     */
    public static final String[] FORBIDDEN_IDENTITY_HEADERS = {
            "X-User-Id", "X-User-Name", "X-Role", "X-Email", "X-Household-Id"
    };

    /** 工具类禁止实例化 */
    private HeaderConstants() {
    }
}
