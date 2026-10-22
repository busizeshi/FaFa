package com.fafa.biz.usage.infrastructure;

import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.biz.usage.domain.QuotaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuotaRepository implements QuotaRepository {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public JdbcQuotaRepository(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public Balance find(String owner) {
        var rows = jdbc.query("SELECT granted,used,reserved,version FROM quota_account WHERE user_id=? AND bucket=?",
                (rs, n) -> new Balance(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)), owner, QuotaAmounts.STORAGE);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void create(String owner, long initial) {
        jdbc.update("INSERT INTO quota_account(user_id,bucket,granted,used,reserved,version) VALUES(?,?,?,0,0,1)", owner, QuotaAmounts.STORAGE, initial);
    }
    public void save(String owner, Balance balance) {
        jdbc.update("UPDATE quota_account SET granted=?,used=?,reserved=?,version=version+1 WHERE user_id=? AND bucket=?",
                balance.granted(), balance.used(), balance.reserved(), owner, QuotaAmounts.STORAGE);
    }
    public Reservation reservation(String id) {
        var rows = jdbc.query("SELECT * FROM quota_reservation WHERE operation_id=?", (rs, n) -> new Reservation(
                rs.getString("operation_id"), rs.getString("user_id"), rs.getLong("amount"),
                rs.getObject("settled_amount", Long.class), rs.getString("state")), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void reserve(String id, String owner, long amount) {
        var now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO quota_reservation(operation_id,user_id,bucket,amount,state,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",
                id, owner, QuotaAmounts.STORAGE, amount, "RESERVED", now, now);
    }
    public void finish(String id, String state, long amount) {
        jdbc.update("UPDATE quota_reservation SET state=?,settled_amount=?,updated_at=? WHERE operation_id=?", state, amount, LocalDateTime.now(clock), id);
    }
    public void ledger(String owner, String id, String type, long amount, String operator, String reason) {
        jdbc.update("INSERT INTO quota_ledger(id,user_id,bucket,operation_id,entry_type,amount,operator_id,reason,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), owner, QuotaAmounts.STORAGE, id, type, amount, operator, reason, LocalDateTime.now(clock));
    }
}
