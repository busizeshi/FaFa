package com.fafa.biz.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.FafaException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 先占用数据库唯一操作标识，再执行业务，操作结果与业务一起提交。 */
@Component
public class AdminOperations {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;
    public AdminOperations(JdbcTemplate jdbc, ObjectMapper json, Clock clock) { this.jdbc = jdbc; this.json = json; this.clock = clock; }
    public <T> T execute(String id, String operator, String operation, Object request, Class<T> type, Supplier<T> work) {
        QuotaAmounts.operation(id);
        String hash = IdempotentWrites.hash(write(request));
        try {
            jdbc.update("INSERT INTO admin_business_operation(operation_id,operator_id,operation,request_hash,request_json,created_at) VALUES(?,?,?,?,?,?)",
                    id, operator, operation, hash, write(request), LocalDateTime.now(clock));
        } catch (DuplicateKeyException exception) {
            var row = jdbc.queryForMap("SELECT * FROM admin_business_operation WHERE operation_id=? FOR UPDATE", id);
            if (!operator.equals(row.get("operator_id")) || !operation.equals(row.get("operation"))
                    || !hash.equals(row.get("request_hash"))) throw new FafaException(BusinessError.IDEMPOTENCY_CONFLICT);
            return read((String) row.get("response_json"), type);
        }
        T response = work.get();
        jdbc.update("UPDATE admin_business_operation SET response_json=? WHERE operation_id=?", write(response), id);
        return response;
    }
    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (java.io.IOException exception) { throw new IllegalStateException("操作结果序列化失败", exception); }
    }
    private <T> T read(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (java.io.IOException exception) { throw new IllegalStateException("操作结果读取失败", exception); }
    }
}
