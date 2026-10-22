package com.fafa.biz.usage.infrastructure;

import com.fafa.common.rpc.AdminBusinessRpc.QuotaRequest;
import com.fafa.biz.usage.domain.QuotaRequestRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 额度申请和对外可见流水的持久化适配器。 */
@Repository
public class QuotaRequestStore implements QuotaRequestRepository {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public QuotaRequestStore(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public QuotaRequest find(String id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM grant_request WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::map, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public List<QuotaRequest> list(String owner, String state, String cursor, int limit) {
        return jdbc.query("SELECT * FROM grant_request WHERE (? IS NULL OR user_id=?) AND (? IS NULL OR state=?) "
                + "AND id>? ORDER BY id LIMIT ?", this::map, owner, owner, state, state, cursor, limit);
    }
    public void insert(String id, String owner, long amount, String reason) {
        var now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO grant_request(id,user_id,bucket,amount,reason,state,version,created_at,updated_at) "
                + "VALUES(?,?,'STORAGE_BYTES',?,?,'PENDING',1,?,?)", id, owner, amount, reason, now, now);
    }
    public void decide(String id, String state, Long amount, String operator, String reason) {
        jdbc.update("UPDATE grant_request SET state=?,granted_amount=?,operator_id=?,decision_reason=?,version=version+1,updated_at=? WHERE id=?",
                state, amount, operator, reason, LocalDateTime.now(clock), id);
    }
    public List<Ledger> ledger(String owner, String cursor, int limit) {
        return jdbc.query("SELECT * FROM quota_ledger WHERE user_id=? AND id>? ORDER BY id LIMIT ?", (rs, n) -> new Ledger(
                rs.getString("id"), rs.getString("operation_id"), rs.getString("entry_type"), Long.toString(rs.getLong("amount")),
                rs.getString("reason"), rs.getTimestamp("created_at").toInstant()), owner, cursor, limit);
    }
    private QuotaRequest map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        var granted = rs.getObject("granted_amount", Long.class);
        return new QuotaRequest(rs.getString("id"), rs.getString("user_id"), rs.getString("bucket"),
                Long.toString(rs.getLong("amount")), rs.getString("reason"), rs.getString("state"),
                granted == null ? null : granted.toString(), rs.getString("decision_reason"), rs.getString("operator_id"), rs.getLong("version"));
    }
}
