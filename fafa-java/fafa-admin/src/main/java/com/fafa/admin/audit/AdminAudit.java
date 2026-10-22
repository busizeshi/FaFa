package com.fafa.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.admin.interfaces.http.AdminWebConfiguration;
import com.fafa.admin.security.AdminLoginLimiter;
import com.fafa.admin.security.AdminPolicy;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.*;
import com.fafa.common.web.RequestIdContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 先提交管理审计意图再调用 Biz；网络故障记录未知结果，重试复用相同操作标识。 */
@Service
@org.springframework.context.annotation.Profile("!admin-init")
public class AdminAudit {
    public record Entry(String operationId, String operatorId, String action, String targetId, String reason,
                        String state, String resultCode, String requestId) { }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final HttpServletRequest request;
    private final Clock clock;
    private final AdminPolicy policy;
    public AdminAudit(JdbcTemplate jdbc, TransactionTemplate tx, ObjectMapper json, HttpServletRequest request, Clock clock, AdminPolicy policy) {
        this.jdbc = jdbc; this.tx = tx; this.json = json; this.request = request; this.clock = clock; this.policy = policy;
    }
    public <T> T execute(String action, String target, String reason, String key, Object body, Function<String,T> work) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}") || reason == null || reason.isBlank() || reason.length() > 500) {
            throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "必须提供幂等键和操作原因");
        }
        String operator = AdminWebConfiguration.current(request).id();
        String id = "admin:" + AdminLoginLimiter.hash(operator + ":" + action + ":" + key);
        String hash = fingerprint(new Intent(action, target, reason, body));
        tx.executeWithoutResult(status -> begin(id, operator, action, target, reason, hash));
        try {
            T result = work.apply(id);
            finish(id, "COMPLETED", "0");
            return result;
        } catch (RpcException exception) {
            finish(id, "UNKNOWN", CommonErrorCode.DEPENDENCY_UNAVAILABLE.code());
            throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE);
        } catch (FafaException exception) {
            finish(id, "REJECTED", exception.errorCode().code());
            throw exception;
        }
    }
    private void begin(String id, String operator, String action, String target, String reason, String hash) {
        var now = LocalDateTime.now(clock);
        try {
            jdbc.update("INSERT INTO admin_audit(operation_id,operator_id,action,target_id,reason,request_hash,state,request_id,created_at,updated_at) "
                    + "VALUES(?,?,?,?,?,?,'PENDING',?,?,?)", id, operator, action, target, reason, hash,
                    RequestIdContext.resolve(org.slf4j.MDC.get(RequestIdContext.MDC_KEY)), now, now);
        } catch (DuplicateKeyException exception) {
            String old = jdbc.queryForObject("SELECT request_hash FROM admin_audit WHERE operation_id=?", String.class, id);
            if (!hash.equals(old)) throw new FafaException(BusinessError.IDEMPOTENCY_CONFLICT);
        }
    }
    private void finish(String id, String state, String code) {
        jdbc.update("UPDATE admin_audit SET state=?,result_code=?,updated_at=? WHERE operation_id=?", state, code, LocalDateTime.now(clock), id);
    }
    public CursorPage<Entry> list(String cursor, int requested) {
        int limit = requested == 0 ? policy.defaultPageSize() : requested;
        if (limit < 1 || limit > policy.maxPageSize()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        var rows = jdbc.query("SELECT * FROM admin_audit WHERE operation_id>? ORDER BY operation_id LIMIT ?", (rs,n) -> new Entry(
                rs.getString("operation_id"), rs.getString("operator_id"), rs.getString("action"), rs.getString("target_id"),
                rs.getString("reason"), rs.getString("state"), rs.getString("result_code"), rs.getString("request_id")), cursor == null ? "" : cursor, limit + 1);
        return new CursorPage<>(rows.stream().limit(limit).toList(), rows.size() > limit ? rows.get(limit - 1).operationId() : null);
    }
    private String fingerprint(Object body) {
        try { return AdminLoginLimiter.hash(json.writeValueAsString(body)); }
        catch (java.io.IOException exception) { throw new IllegalStateException("审计请求序列化失败", exception); }
    }
    private record Intent(String action, String target, String reason, Object body) { }
}
