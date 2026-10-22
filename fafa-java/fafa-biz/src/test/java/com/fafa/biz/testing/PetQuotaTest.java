package com.fafa.biz.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fafa.biz.admin.interfaces.rpc.AdminBusinessRpcProvider;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.biz.usage.infrastructure.JdbcQuotaRepository;
import com.fafa.common.error.FafaException;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import com.fafa.common.rpc.RpcAuthentication;
import java.util.*;
import java.util.concurrent.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class PetQuotaTest extends IdentityHttpSupport {
    @Autowired StorageQuota quota;
    @Autowired RpcAuthentication rpcAuth;
    @Autowired AdminBusinessRpcProvider rpc;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean JdbcQuotaRepository quotaStore;

    @Test void minimalPetBootstrapPreferencesAndOwnership() throws Exception {
        String token = token("pets@example.com");
        var pet = write("POST", "/v1/pets", token, "pet-1", Map.of("nickname", "雪球", "species", "CAT"), 201).path("data");
        String id = pet.path("id").asText();
        assertThat(pet.at("/profile/sex").asText()).isEqualTo("UNKNOWN");
        assertThat(getJson("/v1/bootstrap", token).at("/data/currentPetId").asText()).isEqualTo(id);
        assertThat(getJson("/v1/bootstrap", token).at("/data/nextRoute").asText()).isEqualTo("HOME");
        String stranger = token("stranger@example.com");
        assertThat(getResult("/v1/pets/" + id, stranger).getResponse().getStatus()).isEqualTo(404);
        write("PUT", "/v1/me/pet-preferences", stranger, null, Map.of("version", 1, "currentPetId", id), 404);
        write("PATCH", "/v1/pets/" + id, stranger, null, Map.of("version", 1, "nickname", "窃取"), 404);
        assertThat(getJson("/v1/pets?limit=1", token).at("/data/items").size()).isEqualTo(1);
    }
    @Test void idempotentCreateIsAtomicUnderConcurrentRetries() throws Exception {
        String token = token("concurrent-pet@example.com");
        var body = Map.of("nickname", "雪球", "species", "CAT");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> write("POST", "/v1/pets", token, "same-pet", body, 201));
            var b = pool.submit(() -> write("POST", "/v1/pets", token, "same-pet", body, 201));
            assertThat(a.get(15, TimeUnit.SECONDS).at("/data/id").asText()).isEqualTo(b.get(15, TimeUnit.SECONDS).at("/data/id").asText());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pet", Integer.class)).isEqualTo(1);
        write("POST", "/v1/pets", token, "same-pet", Map.of("nickname", "其他", "species", "CAT"), 409);
        write("POST", "/v1/pets", token, null, body, 400);
    }
    @Test void versionsArchiveDeletionAndWithdrawalPreservePriorState() throws Exception {
        String token = token("lifecycle@example.com");
        String id = write("POST", "/v1/pets", token, "life", Map.of("nickname", "雪球", "species", "CAT"), 201).at("/data/id").asText();
        write("PATCH", "/v1/pets/" + id, token, null, Map.of("version", 1, "nickname", "新名字"), 200);
        write("PATCH", "/v1/pets/" + id, token, null, Map.of("version", 1, "nickname", "旧版本"), 409);
        write("POST", "/v1/pets/" + id + ":archive", token, null, Map.of("version", 2), 200);
        when(clock.instant()).thenReturn(NOW.plus(16, ChronoUnit.DAYS));
        assertThat(getJson("/v1/pets/" + id, token).at("/data/status").asText()).isEqualTo("ARCHIVED");
        assertThat(getJson("/v1/bootstrap", token).at("/data/currentPetId").isNull()).isTrue();
        write("PATCH", "/v1/pets/" + id, token, null, Map.of("version", 3, "nickname", "只读"), 409);
        write("POST", "/v1/pets/" + id + "/deletion-requests", token, "delete-life", Map.of("version", 3, "acknowledgement", true), 202);
        var withdrawn = write("POST", "/v1/pets/" + id + "/deletion-request:withdraw", token, null, Map.of("version", 4), 200);
        assertThat(withdrawn.at("/data/status").asText()).isEqualTo("ARCHIVED");
        write("POST", "/v1/pets/" + id + ":restore", token, null, Map.of("version", 5), 200);
        write("POST", "/v1/pets/" + id + "/deletion-requests", token, "delete-again", Map.of("version", 6, "acknowledgement", true), 202);
        when(clock.instant()).thenReturn(NOW.plus(32, ChronoUnit.DAYS));
        write("POST", "/v1/pets/" + id + "/deletion-request:withdraw", token, null, Map.of("version", 7), 409);
    }
    @Test void speciesDatesAndNullClearingAreValidated() throws Exception {
        String token = token("pet-validation@example.com");
        write("POST", "/v1/pets", token, "numeric-species", Map.of("nickname", "雪球", "species", 0), 400);
        write("POST", "/v1/pets", token, "invalid-breed", Map.of("nickname", "雪球", "species", "CAT", "breedId", "DOG_UNKNOWN"), 400);
        write("POST", "/v1/pets", token, "invalid-date", Map.of("nickname", "雪球", "species", "CAT", "birthDate", "2099-01-01"), 400);
        write("POST", "/v1/pets", token, "invalid-month", Map.of("nickname", "雪球", "species", "CAT", "estimatedBirthMonth", "2026-99"), 400);
        String id = write("POST", "/v1/pets", token, "valid-date", Map.of("nickname", "雪球", "species", "CAT", "birthDate", "2020-01-01"), 201).at("/data/id").asText();
        write("PATCH", "/v1/pets/" + id, token, null, Map.of("version", 1.5, "nickname", "错误版本"), 400);
        var updated = write("PATCH", "/v1/pets/" + id, token, null, Map.of("version", 1, "clearFields", List.of("BIRTH_DATE"), "estimatedBirthMonth", "2020-01"), 200);
        assertThat(updated.at("/data/profile/birthDate").isNull()).isTrue();
        assertThat(getResult("/v1/pets?limit=101", token).getResponse().getStatus()).isEqualTo(400);
        assertThat(getResult("/v1/pets?status=BOGUS", token).getResponse().getStatus()).isEqualTo(400);
    }
    @Test void quotaApprovalRetriesAndLedgerAreExactlyOnce() throws Exception {
        var registered = register("quota@example.com");
        String token = registered.at("/data/session/token").asText();
        String owner = registered.at("/data/user/id").asText();
        assertThat(getJson("/v1/usage", token).at("/data/0/available").asText()).isEqualTo("0");
        String id = write("POST", "/v1/quota-grant-requests", token, "quota-request", Map.of("bucket", "STORAGE_BYTES", "amount", "1000", "reason", "开发测试"), 201).at("/data/id").asText();
        var payload = new QuotaDecision("approve-1", id, true, "1000", "批准测试额度", 1);
        var call = rpcAuth.sign("decideQuota", "admin-1", "QUOTA_WRITE", "req-1", payload);
        assertThat(rpc.decideQuota(call).code()).isEqualTo("0");
        assertThat(rpc.decideQuota(call).data().state()).isEqualTo("APPROVED");
        assertThat(getJson("/v1/usage", token).at("/data/0/granted").asText()).isEqualTo("1000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='GRANT'", Integer.class)).isEqualTo(1);
        assertThat(rpc.decideQuota(rpcAuth.sign("decideQuota", "admin-1", "QUOTA_WRITE", "req-2",
                new QuotaDecision("approve-1", id, true, "2000", "改变内容", 1))).status()).isEqualTo(409);
        assertThat(owner).isNotBlank();
    }
    @Test void concurrentReservationsCannotOverspendAndTransitionsAreIdempotent() throws Exception {
        var registered = register("reserve@example.com");
        String owner = registered.at("/data/user/id").asText();
        assertThat(rpc.grantQuota(rpcAuth.sign("grantQuota", "admin-1", "QUOTA_WRITE", "grant",
                new QuotaGrant("grant-reserve", owner, "STORAGE_BYTES", "1000", "测试预占"))).code()).isEqualTo("0");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> reserve(owner, "upload-1"));
            var b = pool.submit(() -> reserve(owner, "upload-2"));
            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(true, false);
        }
        String operation = jdbc.queryForObject("SELECT operation_id FROM quota_reservation", String.class);
        assertThat(quota.reserve(owner, operation, 700).reserved()).isEqualTo("700");
        assertThat(quota.settle(owner, operation, 600).used()).isEqualTo("600");
        assertThat(quota.settle(owner, operation, 600).reserved()).isEqualTo("0");
        assertThatThrownBy(() -> quota.settle(owner, operation, 500)).isInstanceOf(FafaException.class);
        quota.reserve(owner, "cancel-upload", 400);
        quota.release(owner, "cancel-upload"); quota.release(owner, "cancel-upload");
        assertThat(quota.view(owner).available()).isEqualTo("400");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='SETTLE'", Integer.class)).isEqualTo(1);
    }
    @Test void unsignedOrAlteredRpcIsDeniedAndUserRestrictionInvalidatesSessions() throws Exception {
        var registered = register("disable@example.com");
        String owner = registered.at("/data/user/id").asText();
        String token = registered.at("/data/session/token").asText();
        long version = jdbc.queryForObject("SELECT version FROM user_account WHERE id=?", Long.class, owner);
        var valid = rpcAuth.sign("setUserStatus", "admin-1", "USER_WRITE", "restrict", new UserStatusChange("disable-1", owner, "DISABLED", "测试限制", version));
        assertThat(rpc.setUserStatus(new Call<>(valid.operatorId(), valid.permission(), valid.requestId(), valid.issuedAt(), valid.payload(), "bad")).status()).isEqualTo(403);
        assertThat(rpc.setUserStatus(valid).code()).isEqualTo("0");
        assertThat(getResult("/v1/me", token).getResponse().getStatus()).isEqualTo(403);
        when(clock.instant()).thenReturn(NOW.plusSeconds(121));
        assertThat(rpc.setUserStatus(valid).status()).isEqualTo(403);
    }
    @Test void grantRollbackRemovesBalanceLedgerAndOperationReceipt() throws Exception {
        String owner = register("rollback-quota@example.com").at("/data/user/id").asText();
        quota.view(owner);
        var call = rpcAuth.sign("grantQuota", "admin-1", "QUOTA_WRITE", "rollback",
                new QuotaGrant("rollback-grant", owner, "STORAGE_BYTES", "1000", "回滚验收"));
        doThrow(new org.springframework.dao.TransientDataAccessResourceException("测试账本写入失败"))
                .when(quotaStore).ledger(eq(owner), eq("rollback-grant"), eq("GRANT"), eq(1000L), eq("admin-1"), anyString());
        assertThat(rpc.grantQuota(call).status()).isEqualTo(503);
        assertThat(quota.view(owner).granted()).isEqualTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_business_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger", Integer.class)).isZero();
        doCallRealMethod().when(quotaStore).ledger(eq(owner), eq("rollback-grant"), eq("GRANT"), eq(1000L), eq("admin-1"), anyString());
        assertThat(rpc.grantQuota(call).data().granted()).isEqualTo("1000");
    }
    @Test void concurrentRpcRetryAndConcurrentDecisionsNeverDoubleGrant() throws Exception {
        var registered = register("approve-race@example.com");
        String token = registered.at("/data/session/token").asText();
        String owner = registered.at("/data/user/id").asText();
        String id = write("POST", "/v1/quota-grant-requests", token, "race-request",
                Map.of("bucket", "STORAGE_BYTES", "amount", "1000", "reason", "审批并发验收"), 201).at("/data/id").asText();
        var call = rpcAuth.sign("decideQuota", "admin-1", "QUOTA_WRITE", "race",
                new QuotaDecision("same-decision", id, true, "1000", "同一审批操作重试", 1));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> rpc.decideQuota(call));
            var b = pool.submit(() -> rpc.decideQuota(call));
            assertThat(List.of(a.get(15, TimeUnit.SECONDS).code(), b.get(15, TimeUnit.SECONDS).code())).containsOnly("0");
        }
        assertThat(rpc.decideQuota(rpcAuth.sign("decideQuota", "admin-2", "QUOTA_WRITE", "other",
                new QuotaDecision("other-decision", id, true, "1000", "不同审批人重复批准", 1))).status()).isEqualTo(409);
        assertThat(quota.view(owner).granted()).isEqualTo("1000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='GRANT'", Integer.class)).isEqualTo(1);
    }
    @Test void paginationHasStableCursorAndDoesNotOmitSelectedPetFromBootstrap() throws Exception {
        String token = token("paging@example.com");
        var ids = new ArrayList<String>();
        for (int i = 0; i < 22; i++) ids.add(write("POST", "/v1/pets", token, "pet-" + i,
                Map.of("nickname", "宠物" + i, "species", "CAT"), 201).at("/data/id").asText());
        ids.sort(String::compareTo);
        var page = getJson("/v1/pets?limit=20", token).path("data");
        assertThat(page.path("items").size()).isEqualTo(20);
        var second = getJson("/v1/pets?limit=20&cursor=" + page.path("nextCursor").asText(), token).path("data");
        assertThat(second.path("items").size()).isEqualTo(2);
        assertThat(second.path("nextCursor").isNull()).isTrue();
        long version = getJson("/v1/me/pet-preferences", token).at("/data/version").asLong();
        write("PUT", "/v1/me/pet-preferences", token, null, Map.of("version", version, "currentPetId", ids.getLast()), 200);
        var bootstrap = getJson("/v1/bootstrap", token).path("data");
        assertThat(bootstrap.path("currentPetId").asText()).isEqualTo(ids.getLast());
        assertThat(bootstrap.path("pets").findValuesAsText("id")).contains(ids.getLast());
    }
    private boolean reserve(String owner, String operation) {
        try { quota.reserve(owner, operation, 700); return true; }
        catch (FafaException exception) { assertThat(exception.errorCode().code()).isEqualTo("FAFA-40913"); return false; }
    }
    private String token(String email) throws Exception { return register(email).at("/data/session/token").asText(); }
    private JsonNode write(String method, String path, String token, String key, Object body, int status) throws Exception {
        var request = switch (method) { case "PATCH" -> patch(path); case "PUT" -> put(path); default -> post(path); };
        request.header("Authorization", "Bearer " + token).contentType("application/json").content(json.writeValueAsBytes(body));
        if (key != null) request.header("Idempotency-Key", key);
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }
}
