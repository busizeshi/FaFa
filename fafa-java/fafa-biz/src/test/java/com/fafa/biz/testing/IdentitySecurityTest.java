package com.fafa.biz.testing;

import com.fafa.biz.identity.application.AuthRateLimitException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class IdentitySecurityTest extends IdentityHttpSupport {
    @Test void emailCanonicalizationPreservesPlusAndCaseInsensitiveUniqueness() throws Exception {
        register(" Pet.Owner+tag@Example.com ");
        String id = challenge("REGISTER", "pet.owner+tag@example.com");
        registerWith("pet.owner+tag@example.com", id, codes.get("pet.owner+tag@example.com"), LEGAL, 409);
        assertThat(jdbc.queryForObject("SELECT email_canonical FROM user_account", String.class))
                .isEqualTo("pet.owner+tag@example.com");
    }
    @Test void concurrentLoginsLeaveExactlyOneUsableToken() throws Exception {
        register("loginrace@example.com");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.<Callable<String>>of(
                    () -> postJson("/v1/auth/sessions", Map.of("email", "loginrace@example.com",
                            "password", PASSWORD, "device", device("a")), 200).at("/data/session/token").asText(),
                    () -> postJson("/v1/auth/sessions", Map.of("email", "loginrace@example.com",
                            "password", PASSWORD, "device", device("b")), 200).at("/data/session/token").asText());
            var results = executor.invokeAll(requests);
            assertThat(List.of(getResult("/v1/me", results.get(0).get()).getResponse().getStatus(),
                    getResult("/v1/me", results.get(1).get()).getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 401);
        }
    }
    @Test void passwordResetFailsClosedEvenIfRedisCleanupFails() throws Exception {
        String token = register("generation@example.com").at("/data/session/token").asText();
        String id = challenge("RESET_PASSWORD", "generation@example.com");
        String ticket = postJson("/v1/auth/password-reset-tickets",
                Map.of("challengeId", id, "code", codes.get("generation@example.com")), 200).at("/data/resetTicket").asText();
        doThrow(new DataAccessResourceFailureException("not available")).when(sessions).revokeAll(anyString());
        postJson("/v1/auth/password-resets", Map.of("resetTicket", ticket,
                "password", "Changed12345", "confirmedPassword", "Changed12345"), 200);
        assertThat(getResult("/v1/me", token).getResponse().getStatus()).isEqualTo(401);
    }
    @Test void expiredTicketCannotChangePassword() throws Exception {
        register("ticket@example.com");
        String id = challenge("RESET_PASSWORD", "ticket@example.com");
        String ticket = postJson("/v1/auth/password-reset-tickets",
                Map.of("challengeId", id, "code", codes.get("ticket@example.com")), 200).at("/data/resetTicket").asText();
        when(clock.instant()).thenReturn(NOW.plusSeconds(600));
        postJson("/v1/auth/password-resets", Map.of("resetTicket", ticket,
                "password", "Changed12345", "confirmedPassword", "Changed12345"), 410);
    }
    @Test void rateLimitResponseIncludesRetryAfterAndDoesNotSendMail() throws Exception {
        doThrow(new AuthRateLimitException(37)).when(limiter).challenge(anyString(), anyString(), any());
        var response = postResult("/v1/auth/email-challenges",
                Map.of("purpose", "REGISTER", "email", "limited@example.com")).getResponse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("37");
        verify(mail, never()).send(anyString(), anyString(), anyInt());
    }
    @Test void logoutRevokesTokenAndAnonymousBootstrapContainsNoPrivateUser() throws Exception {
        String token = register("logout@example.com").at("/data/session/token").asText();
        var logout = mvc.perform(delete("/v1/auth/session").header("Authorization", "Bearer " + token)).andReturn();
        assertThat(logout.getResponse().getStatus()).isEqualTo(200);
        assertThat(getResult("/v1/me", token).getResponse().getStatus()).isEqualTo(401);
        var anonymous = mvc.perform(get("/v1/bootstrap")).andReturn();
        assertThat(json.readTree(anonymous.getResponse().getContentAsString()).at("/data/user").isNull()).isTrue();
    }
    @Test void publishedLegalVersionsAreReadableAndUnpublishedDocumentsAreNot() throws Exception {
        var response = mvc.perform(get("/v1/legal-documents").param("type", "TERMS").param("version", "v1")).andReturn();
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(response.getResponse().getContentAsString())
                .at("/data/0/effectiveAt").asText()).endsWith("Z");
        var missing = mvc.perform(get("/v1/legal-documents").param("type", "TERMS").param("version", "draft")).andReturn();
        assertThat(missing.getResponse().getStatus()).isEqualTo(404);
    }
}
