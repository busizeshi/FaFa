package com.fafa.admin.testing;

import com.fasterxml.jackson.databind.*;
import com.fafa.admin.security.AdminSessions;
import com.fafa.admin.infrastructure.rpc.AdminBusinessClient;
import com.fafa.common.rpc.AdminBusinessRpc;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import com.fafa.common.rpc.RpcAuthentication;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = AdminTestApplication.class, properties = "spring.config.location=classpath:admin-test.yml")
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    @Autowired AdminSessions sessions;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminBusinessClient client;
    @Autowired RpcAuthentication rpcAuth;
    @MockitoBean Clock clock;
    private final AdminBusinessRpc remote = mock(AdminBusinessRpc.class);
    static final String PASSWORD = "AdminTestingPassword123";
    static final Instant NOW = Instant.parse("2026-10-21T04:00:00Z");
    @BeforeAll void schema() throws Exception {
        String sql = new ClassPathResource("db/admin-migration/V001__admin_security.sql")
                .getContentAsString(StandardCharsets.UTF_8).replace("CHARACTER SET ascii COLLATE ascii_bin", "")
                .replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ").replace("INSERT INTO ", "INSERT IGNORE INTO ");
        try (var connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.support.EncodedResource(
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        }
    }
    @BeforeEach void clean() {
        for (String table : List.of("admin_session", "admin_account_role", "admin_account", "admin_login_throttle", "admin_audit")) {
            jdbc.update("DELETE FROM " + table);
        }
        when(clock.instant()).thenReturn(NOW); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        reset(remote);
        ReflectionTestUtils.setField(client, "remote", remote);
        sessions.bootstrap("root-admin", PASSWORD);
    }
    @Test void credentialsSessionExpiryAndLogoutAreIsolated() throws Exception {
        String token = login("root-admin", PASSWORD, 200).at("/data/token").asText();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM admin_session", String.class)).isNotEqualTo(token);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM admin_account", String.class)).startsWith("$argon2id$");
        assertThat(get("/v1/me", token, 200).at("/data/permissions").size()).isEqualTo(7);
        get("/v1/me", "mobile-token", 401);
        when(clock.instant()).thenReturn(NOW.plusSeconds(3601));
        get("/v1/me", token, 401);
        when(clock.instant()).thenReturn(NOW);
        var response = mvc.perform(delete("/v1/auth/session").header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        get("/v1/me", token, 401);
        assertThatThrownBy(() -> sessions.bootstrap("root-admin", "NewAdminPassword123")).isInstanceOf(IllegalStateException.class);
    }
    @Test void permissionRevocationTakesEffectForExistingSessionAndSpoofedHeadersDoNotHelp() throws Exception {
        String token = login("root-admin", PASSWORD, 200).at("/data/token").asText();
        String id = jdbc.queryForObject("SELECT id FROM admin_account", String.class);
        jdbc.update("DELETE FROM admin_account_role WHERE admin_id=?", id);
        jdbc.update("INSERT INTO admin_account_role(admin_id,role_id) VALUES(?,'SUPPORT')", id);
        when(remote.users(any())).thenReturn(new Reply<>("0", "成功", 200, new com.fafa.common.api.CursorPage<>(List.of(), null)));
        get("/v1/users", token, 200);
        get("/v1/audit-logs", token, 403);
        var response = mvc.perform(post("/v1/users/user-1/quota-adjustments").header("Authorization", "Bearer " + token)
                .header("X-Operator-ID", "root-admin").header("Idempotency-Key", "spoof")
                .contentType("application/json").content(json.writeValueAsBytes(Map.of("bucket", "STORAGE_BYTES", "amount", "100", "entryType", "GRANT", "reason", "越权测试"))))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);
        verify(remote, never()).grantQuota(any());
    }
    @Test void failedLoginsRemainRateLimitedAcrossRequests() throws Exception {
        for (int i = 0; i < 10; i++) login("root-admin", "WrongPassword123", 401);
        var response = mvc.perform(post("/v1/auth/sessions").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("username", "root-admin", "password", PASSWORD)))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        when(clock.instant()).thenReturn(NOW.plusSeconds(901));
        login("root-admin", PASSWORD, 200);
    }
    @Test void auditedRpcIsSignedAndRetryReusesOperationId() throws Exception {
        String token = login("root-admin", PASSWORD, 200).at("/data/token").asText();
        when(remote.grantQuota(any())).thenAnswer(invocation -> {
            Call<QuotaGrant> call = invocation.getArgument(0);
            rpcAuth.verify("grantQuota", "QUOTA_WRITE", call);
            return new Reply<>("0", "成功", 200, new QuotaView("STORAGE_BYTES", "100", "0", "0", "100", 2));
        });
        var body = Map.of("bucket", "STORAGE_BYTES", "amount", "100", "entryType", "GRANT", "reason", "测试赠送");
        writeGrant(token, "grant-once", body, 200); writeGrant(token, "grant-once", body, 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT state FROM admin_audit", String.class)).isEqualTo("COMPLETED");
        writeGrant(token, "grant-once", Map.of("bucket", "STORAGE_BYTES", "amount", "200", "entryType", "GRANT", "reason", "更改额度"), 409);
        verify(remote, times(2)).grantQuota(any());
    }
    @Test void networkUncertaintyIsAuditedAndCanBeRetried() throws Exception {
        String token = login("root-admin", PASSWORD, 200).at("/data/token").asText();
        when(remote.grantQuota(any())).thenThrow(new org.apache.dubbo.rpc.RpcException("测试断网"));
        var body = Map.of("bucket", "STORAGE_BYTES", "amount", "100", "entryType", "GRANT", "reason", "测试赠送");
        writeGrant(token, "uncertain", body, 503);
        assertThat(jdbc.queryForObject("SELECT state FROM admin_audit", String.class)).isEqualTo("UNKNOWN");
        doReturn(new Reply<>("0", "成功", 200, new QuotaView("STORAGE_BYTES", "100", "0", "0", "100", 2))).when(remote).grantQuota(any());
        writeGrant(token, "uncertain", body, 200);
        assertThat(jdbc.queryForObject("SELECT state FROM admin_audit", String.class)).isEqualTo("COMPLETED");
    }
    @Test void runtimeOpenApiCanBeExportedAndOnlyBusinessV1RoutesArePublished() throws Exception {
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        var document = json.readTree(response.getContentAsString());
        assertThat(document.at("/paths/~1v1~1users/get/operationId").asText()).isEqualTo("listAdminUsers");
        Files.writeString(Path.of("target", "admin-openapi.json"), response.getContentAsString(), StandardCharsets.UTF_8);
        try (var input = Files.newInputStream(Path.of("..", "..", "contracts", "openapi", "admin-v1.yaml"))) {
            var yaml = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));
            var paths = json.valueToTree(yaml.load(input)).path("paths").fields();
            int count = 0;
            while (paths.hasNext()) {
                var entry = paths.next();
                var methods = entry.getValue().fields();
                while (methods.hasNext()) {
                    var method = methods.next();
                    assertThat(document.path("paths").path("/v1" + entry.getKey()).path(method.getKey()).path("operationId").asText())
                            .isEqualTo(method.getValue().path("operationId").asText());
                    count++;
                }
            }
            assertThat(count).isEqualTo(13);
        }
    }
    private JsonNode login(String username, String password, int status) throws Exception {
        var response = mvc.perform(post("/v1/auth/sessions").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("username", username, "password", password)))).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }
    private JsonNode get(String path, String token, int status) throws Exception {
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("Authorization", "Bearer " + token))
                .andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }
    private void writeGrant(String token, String key, Object body, int status) throws Exception {
        var response = mvc.perform(post("/v1/users/user-1/quota-adjustments").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key).contentType("application/json").content(json.writeValueAsBytes(body))).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    }
}
