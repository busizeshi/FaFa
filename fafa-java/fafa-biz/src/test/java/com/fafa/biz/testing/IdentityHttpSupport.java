package com.fafa.biz.testing;

import com.fasterxml.jackson.databind.*;
import com.fafa.biz.identity.application.AuthRateLimiter;
import com.fafa.biz.identity.application.VerificationMailSender;
import com.fafa.biz.identity.infrastructure.SaTokenSessions;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = IdentityTestApplication.class,
        properties = "spring.config.location=classpath:identity-test.yml")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IdentityHttpSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @MockitoBean AuthRateLimiter limiter;
    @MockitoBean VerificationMailSender mail;
    @MockitoBean Clock clock;
    @MockitoSpyBean SaTokenSessions sessions;
    final Map<String, String> codes = new ConcurrentHashMap<>();
    static final Instant NOW = Instant.parse("2026-10-21T04:00:00Z");
    static final String PASSWORD = "PetPassword123";
    static final Map<String, String> LEGAL = Map.of("TERMS", "v1", "PRIVACY", "v1");

    @BeforeAll void schema() throws Exception {
        for (String path : List.of("db/migration/V001__identity.sql", "db/migration/V002__auth_challenges.sql",
                "db/migration/V003__pets.sql", "db/migration/V004__storage_quotas.sql", "db/migration/V005__media_upload.sql", "db/migration/V006__media_library.sql", "db/migration/V007__media_processing.sql")) {
            if (path.endsWith("V005__media_upload.sql") && jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)='upload_session'", Integer.class) > 0) continue;
            if(path.endsWith("V006__media_library.sql") && jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='media_asset' AND LOWER(column_name)='media_type'",Integer.class)>0) continue;
            if(path.endsWith("V007__media_processing.sql") && jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)='media_processing_job'",Integer.class)>0) continue;
            String sql = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8)
                    .replace("CHARACTER SET utf8mb4 COLLATE utf8mb4_bin", "")
                    .replace("CHARACTER SET ascii COLLATE ascii_bin", "")
                    .replace("INSERT INTO pet_breed", "INSERT IGNORE INTO pet_breed")
                    .replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
            try (var connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection,
                        new org.springframework.core.io.support.EncodedResource(
                                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            }
        }
    }
    @BeforeEach void clean() {
        for (String table : List.of("media_reference", "consumer_inbox", "outbox_event", "media_processing_job", "album_item", "album", "media_tag", "media_binding", "media_asset", "upload_session", "admin_business_operation", "quota_ledger", "quota_reservation", "quota_account", "grant_request",
                "idempotency_request", "pet_deletion_request", "user_preferences", "pet", "user_consent", "user_profile", "auth_ticket", "auth_challenge",
                "security_audit", "user_account", "legal_document")) jdbc.update("DELETE FROM " + table);
        for (String type : LEGAL.keySet()) jdbc.update("""
            INSERT INTO legal_document(document_type,version,title,body,content_hash,status,effective_at)
            VALUES(?, 'v1', 'Test only', 'Development fixture', 'hash', 'PUBLISHED', '2020-01-01 00:00:00')
            """, type);
        codes.clear();
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        doAnswer(call -> { codes.put(call.getArgument(0, String.class).toLowerCase(Locale.ROOT),
                call.getArgument(1)); return null; }).when(mail).send(anyString(), anyString(), anyInt());
    }
    Map<String, String> device(String id) { return Map.of("deviceId", id, "deviceName", "Android test"); }
    JsonNode register(String email) throws Exception {
        String id = challenge("REGISTER", email);
        return registerWith(email, id, codes.get(email.strip().toLowerCase(Locale.ROOT)), LEGAL, 201);
    }
    String challenge(String purpose, String email) throws Exception {
        return postJson("/v1/auth/email-challenges", Map.of("purpose", purpose, "email", email), 202)
                .at("/data/challengeId").asText();
    }
    Map<String, Object> registration(String email, String id, String code, Map<String, String> legal) {
        return Map.of("email", email, "password", PASSWORD, "confirmedPassword", PASSWORD,
                "challengeId", id, "code", code, "legalVersions", legal, "device", device("first"));
    }
    JsonNode registerWith(String email, String id, String code, Map<String, String> legal, int expected) throws Exception {
        return postJson("/v1/auth/registrations", registration(email, id, code, legal), expected);
    }
    MvcResult postResult(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType("application/json").content(json.writeValueAsBytes(body))).andReturn();
    }
    JsonNode postJson(String path, Object body, int status) throws Exception {
        MvcResult result = postResult(path, body);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        return json.readTree(result.getResponse().getContentAsString());
    }
    MvcResult getResult(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();
    }
    JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = getResult(path, token);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString());
    }
}
