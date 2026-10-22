package com.fafa.biz.identity.infrastructure.persistence;

import com.fafa.common.rpc.AdminBusinessRpc.UserSummary;
import com.fafa.biz.identity.domain.UserDirectory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 后台只读取脱敏账号摘要；私人业务正文不进入此查询。 */
@Repository
public class AdminUserQueries implements UserDirectory {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public AdminUserQueries(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public List<UserSummary> list(String cursor, int limit) {
        return jdbc.query("SELECT a.id,a.email_display,p.nickname,a.status,a.version FROM user_account a "
                + "JOIN user_profile p ON p.user_id=a.id WHERE a.id>? ORDER BY a.id LIMIT ?", this::map, cursor, limit);
    }
    public UserSummary find(String id) {
        var rows = jdbc.query("SELECT a.id,a.email_display,p.nickname,a.status,a.version FROM user_account a "
                + "JOIN user_profile p ON p.user_id=a.id WHERE a.id=?", this::map, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void status(String id, String state) {
        jdbc.update("UPDATE user_account SET status=?,auth_version=auth_version+1,version=version+1,updated_at=? WHERE id=?",
                state, LocalDateTime.now(clock), id);
    }
    private UserSummary map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        String email = rs.getString("email_display");
        int separator = email.indexOf('@');
        String masked = separator < 0 ? "***" : email.substring(0, 1) + "***" + email.substring(separator);
        return new UserSummary(rs.getString("id"), masked, rs.getString("nickname"), rs.getString("status"), rs.getLong("version"));
    }
}
