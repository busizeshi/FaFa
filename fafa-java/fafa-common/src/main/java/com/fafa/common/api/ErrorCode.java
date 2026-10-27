package com.fafa.common.api;

/**
 * 稳定业务错误码契约（架构规范 §6.1 / §9.1）。
 *
 * <p>错误码是跨服务、跨端的<strong>对外契约</strong>，一经发布不得变更语义，只能新增。
 * 编码规则（与 HTTP 状态对齐，便于排查）：</p>
 * <pre>
 *   [HTTP 状态段][服务域][序号]  例如 401-01-00 表示认证类错误
 * </pre>
 * <p>各服务在 {@code interfaces} 层实现各自的 ErrorCode 枚举时必须实现本接口，
 * 禁止在 Controller 中直接返回字符串拼接的错误信息。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public interface ErrorCode {

    /**
     * @return 稳定业务错误码，如 {@code "401-01-00"}；成功固定为 {@code "0"}
     */
    String getCode();

    /**
     * @return 面向调用方的默认中文提示；对外必须稳定，不得携带内部实现细节（SQL、主机名、堆栈等）
     */
    String getMessage();
}
