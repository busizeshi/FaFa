package com.fafa.admin.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 仅连接管理数据库的身份持久化适配器。 */
@Repository
public class AdminStore {
    public record Account(String id, String username, String passwordHash, String status, long authVersion) {
        @Override public String toString() { return "AdminAccount[id=" + id + "]"; }
    }
    private final JdbcTemplate jdbc;
    public AdminStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Account username(String username) {
        var rows = jdbc.query("SELECT * FROM admin_account WHERE username=?", this::map, username);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Account session(String hash, Instant now) {
        var rows = jdbc.query("SELECT a.* FROM admin_session s JOIN admin_account a ON a.id=s.admin_id "
                + "WHERE s.token_hash=? AND s.expires_at>? AND s.auth_version=a.auth_version AND a.status='ACTIVE'",
                this::map, hash, utc(now));
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Set<String> permissions(String id) {
        return new HashSet<>(jdbc.query("SELECT DISTINCT p.permission_code FROM admin_role_permission p "
                + "JOIN admin_account_role r ON r.role_id=p.role_id WHERE r.admin_id=?", (rs, n) -> rs.getString(1), id));
    }
    public void session(String hash, Account account, Instant now, Instant expires) {
        jdbc.update("INSERT INTO admin_session(token_hash,admin_id,auth_version,expires_at,created_at) VALUES(?,?,?,?,?)",
                hash, account.id(), account.authVersion(), utc(expires), utc(now));
    }
    public void logout(String hash) { jdbc.update("DELETE FROM admin_session WHERE token_hash=?", hash); }
    public void bootstrap(String username, String passwordHash, Instant now) {
        jdbc.queryForObject("SELECT id FROM admin_bootstrap_lock WHERE id=1 FOR UPDATE", Integer.class);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM admin_account", Long.class) != 0) {
            throw new IllegalStateException("已有管理员，拒绝再次初始化或重置密码");
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO admin_account(id,username,password_hash,status,auth_version,created_at) VALUES(?,?,?,'ACTIVE',1,?)",
                id, username, passwordHash, utc(now));
        jdbc.update("INSERT INTO admin_account_role(admin_id,role_id) VALUES(?,'SUPER_ADMIN')", id);
    }
    private Account map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Account(rs.getString("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("status"), rs.getLong("auth_version"));
    }
    private LocalDateTime utc(Instant value) { return LocalDateTime.ofInstant(value, ZoneOffset.UTC); }
}
