package com.fafa.admin.infrastructure.rpc;

import com.fafa.admin.interfaces.http.AdminWebConfiguration;
import com.fafa.common.error.*;
import com.fafa.common.rpc.AdminBusinessRpc;
import com.fafa.common.rpc.RpcAuthentication;
import com.fafa.common.web.RequestIdContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.function.BiFunction;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 签名、网络调用和错误转换统一封装；业务服务不接触 Dubbo 引用细节。 */
@Component
@org.springframework.context.annotation.Profile("!admin-init")
public class AdminBusinessClient {
    @DubboReference(check = false, retries = 0, lazy = true, url = "${fafa.biz-rpc.url:}")
    private AdminBusinessRpc remote;
    private final RpcAuthentication auth;
    private final HttpServletRequest request;
    public AdminBusinessClient(RpcAuthentication auth, HttpServletRequest request) { this.auth = auth; this.request = request; }
    public <P,T> T call(String method, String permission, P payload,
                       BiFunction<AdminBusinessRpc, AdminBusinessRpc.Call<P>, AdminBusinessRpc.Reply<T>> invoke) {
        var actor = AdminWebConfiguration.current(request);
        if (!actor.permissions().contains(permission)) throw new FafaException(CommonErrorCode.FORBIDDEN);
        var signed = auth.sign(method, actor.id(), permission, RequestIdContext.resolve(org.slf4j.MDC.get(RequestIdContext.MDC_KEY)), payload);
        var reply = invoke.apply(remote, signed);
        if (reply == null) throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE);
        if (!"0".equals(reply.code())) {
            throw new FafaException(new RemoteError(reply.code(), reply.message(), HttpStatus.valueOf(reply.status())));
        }
        return reply.data();
    }
    private record RemoteError(String code, String message, HttpStatus httpStatus) implements ErrorCode { }
}
