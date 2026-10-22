package com.fafa.biz.operation;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 仅缓存非敏感业务结果；调用前必须在同一事务内取得当前用户写锁。 */
@Component
public class IdempotentWrites {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;
    public IdempotentWrites(JdbcTemplate jdbc, ObjectMapper json, Clock clock) {
        this.jdbc = jdbc; this.clock = clock;
        this.json = json.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }
    public <T> T execute(String actor, String operation, String key, Object request,
                         Class<T> resultType, Supplier<T> action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("幂等写入必须处于事务中");
        }
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "请提供有效的 Idempotency-Key");
        }
        String hash = hash(serialize(request));
        var rows = jdbc.query("SELECT request_hash,response_json FROM idempotency_request "
                + "WHERE actor_id=? AND operation=? AND request_key=?", (rs, n) ->
                new Stored(rs.getString(1), rs.getString(2)), actor, operation, key);
        if (!rows.isEmpty()) {
            if (!rows.getFirst().hash().equals(hash)) throw new FafaException(BusinessError.IDEMPOTENCY_CONFLICT);
            return deserialize(rows.getFirst().response(), resultType);
        }
        T result = action.get();
        jdbc.update("INSERT INTO idempotency_request(actor_id,operation,request_key,request_hash,response_json,created_at) "
                + "VALUES(?,?,?,?,?,?)", actor, operation, key, hash, serialize(result), LocalDateTime.now(clock));
        return result;
    }
    private record Stored(String hash, String response) { }
    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (java.io.IOException exception) { throw new IllegalStateException("业务结果序列化失败", exception); }
    }
    private <T> T deserialize(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (java.io.IOException exception) { throw new IllegalStateException("幂等结果读取失败", exception); }
    }
}
