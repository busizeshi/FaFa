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

class IdentityHttpTest extends IdentityHttpSupport {
    @Test void registrationLoginBootstrapAndSingleDeviceReplacement() throws Exception {
        JsonNode registered = register("Owner@Example.com");
        String first = registered.at("/data/session/token").asText();
        assertThat(first).isNotBlank();
        assertThat(getJson("/v1/me", first).at("/data/email").asText()).isEqualTo("Owner@Example.com");
        JsonNode login = postJson("/v1/auth/sessions", Map.of("email", "owner@example.com",
                "password", PASSWORD, "device", device("second")), 200);
        String second = login.at("/data/session/token").asText();
        assertThat(second).isNotEqualTo(first);
        assertThat(getResult("/v1/me", first).getResponse().getStatus()).isEqualTo(401);
        assertThat(getJson("/v1/bootstrap", second).at("/data/nextRoute").asText()).isEqualTo("NO_PET");
    }
    @Test void wrongAttemptsCommitAndEventuallyRejectEvenCorrectCode() throws Exception {
        String id = challenge("REGISTER", "attempts@example.com");
        String actual = codes.get("attempts@example.com");
        String wrong = actual.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) registerWith("attempts@example.com", id, wrong, LEGAL, 400);
        assertThat(jdbc.queryForObject("SELECT attempts FROM auth_challenge WHERE id=?", Integer.class, id)).isEqualTo(5);
        registerWith("attempts@example.com", id, actual, LEGAL, 400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Integer.class)).isZero();
    }
    @Test void resendInvalidatesOldChallengeAndChallengesAreSingleUse() throws Exception {
        String first = challenge("REGISTER", "resend@example.com");
        String oldCode = codes.get("resend@example.com");
        String second = challenge("REGISTER", "resend@example.com");
        registerWith("resend@example.com", first, oldCode, LEGAL, 400);
        registerWith("resend@example.com", second, codes.get("resend@example.com"), LEGAL, 201);
        registerWith("resend@example.com", second, codes.get("resend@example.com"), LEGAL, 400);
    }
    @Test void expiredAndWrongPurposeChallengesCannotRegister() throws Exception {
        String id = challenge("REGISTER", "expiry@example.com");
        when(clock.instant()).thenReturn(NOW.plusSeconds(600));
        registerWith("expiry@example.com", id, codes.get("expiry@example.com"), LEGAL, 410);
        when(clock.instant()).thenReturn(NOW);
        register("purpose@example.com");
        String reset = challenge("RESET_PASSWORD", "purpose@example.com");
        registerWith("purpose@example.com", reset, codes.get("purpose@example.com"), LEGAL, 400);
    }
    @Test void staleAgreementDoesNotConsumeChallenge() throws Exception {
        String id = challenge("REGISTER", "legal@example.com");
        registerWith("legal@example.com", id, codes.get("legal@example.com"), Map.of("TERMS", "old", "PRIVACY", "v1"), 409);
        assertThat(jdbc.queryForObject("SELECT consumed_at FROM auth_challenge WHERE id=?", Object.class, id)).isNull();
        registerWith("legal@example.com", id, codes.get("legal@example.com"), LEGAL, 201);
    }
    @Test void resetRevokesOldSessionAndTicketCannotReplay() throws Exception {
        String token = register("reset@example.com").at("/data/session/token").asText();
        String id = challenge("RESET_PASSWORD", "reset@example.com");
        JsonNode verified = postJson("/v1/auth/password-reset-tickets",
                Map.of("challengeId", id, "code", codes.get("reset@example.com")), 200);
        String ticket = verified.at("/data/resetTicket").asText();
        Map<String, String> reset = Map.of("resetTicket", ticket, "password", "NewPassword456",
                "confirmedPassword", "NewPassword456");
        postJson("/v1/auth/password-resets", reset, 200);
        assertThat(getResult("/v1/me", token).getResponse().getStatus()).isEqualTo(401);
        postJson("/v1/auth/password-resets", reset, 410);
        postJson("/v1/auth/sessions", Map.of("email", "reset@example.com", "password", PASSWORD, "device", device("old")), 401);
        postJson("/v1/auth/sessions", Map.of("email", "reset@example.com", "password", "NewPassword456", "device", device("new")), 200);
    }
    @Test void unknownEmailResetDoesNotSendOrExposeAccount() throws Exception {
        challenge("RESET_PASSWORD", "missing@example.com");
        verify(mail, never()).send(anyString(), anyString(), anyInt());
        assertThat(jdbc.queryForObject("SELECT send_state FROM auth_challenge", String.class)).isEqualTo("SUPPRESSED");
    }
    @Test void smtpFailureDoesNotCreateUsableChallenge() throws Exception {
        doThrow(new MailSendException("sensitive infrastructure failure")).when(mail).send(anyString(), anyString(), anyInt());
        JsonNode error = postJson("/v1/auth/email-challenges", Map.of("purpose", "REGISTER", "email", "mail@example.com"), 503);
        assertThat(error.toString()).doesNotContain("sensitive infrastructure");
        assertThat(jdbc.queryForObject("SELECT send_state FROM auth_challenge", String.class)).isEqualTo("FAILED");
    }
    @Test void anonymousSpoofedHeadersAndUnknownFieldsCannotBypassAuthentication() throws Exception {
        var spoof = mvc.perform(get("/v1/me").header("X-User-Id", "victim")).andReturn();
        assertThat(spoof.getResponse().getStatus()).isEqualTo(401);
        JsonNode error = postJson("/v1/auth/sessions", Map.of("email", "x@example.com",
                "password", PASSWORD, "device", device("x"), "ownerUserId", "victim"), 400);
        assertThat(error.at("/data/fieldErrors/0/field").asText()).isEqualTo("request");
        assertThat(error.toString()).doesNotContain(PASSWORD);
    }
    @Test void concurrentRegistrationCreatesOneAccountAndConsentPair() throws Exception {
        String id = challenge("REGISTER", "parallel@example.com");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var body = registration("parallel@example.com", id, codes.get("parallel@example.com"), LEGAL);
            List<Future<Integer>> futures = executor.invokeAll(List.of(
                    () -> postResult("/v1/auth/registrations", body).getResponse().getStatus(),
                    () -> postResult("/v1/auth/registrations", body).getResponse().getStatus()));
            assertThat(List.of(futures.get(0).get(), futures.get(1).get())).containsExactlyInAnyOrder(201, 400);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_consent", Integer.class)).isEqualTo(2);
    }
    @Test void redisFailureAfterAccountCommitGivesExplicitLoginRecovery() throws Exception {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("redis down"))
                .when(sessions).issue(anyString(), anyLong(), any());
        JsonNode result = register("recovery@example.com");
        assertThat(result.at("/data/nextAction").asText()).isEqualTo("LOGIN");
        assertThat(result.at("/data/session").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Integer.class)).isEqualTo(1);
    }
    @Test void requestIdAndFieldErrorsUseSharedEnvelope() throws Exception {
        var result = mvc.perform(post("/v1/auth/sessions").header("X-Request-ID", "safe-id")
                .contentType("application/json").content("{}")).andReturn();
        JsonNode error = json.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(error.path("requestId").asText()).isEqualTo("safe-id");
        assertThat(error.at("/data/fieldErrors").isArray()).isTrue();
    }
}
