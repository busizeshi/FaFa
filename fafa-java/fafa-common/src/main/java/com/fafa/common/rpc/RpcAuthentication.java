package com.fafa.common.rpc;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.FafaException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 对操作名、操作者、权限、请求标识、时间和完整载荷签名，防止跨方法重放。 */
public final class RpcAuthentication {
    private final String secret;
    private final Clock clock;
    private final long maxSkewSeconds;
    private final ObjectMapper json;
    public RpcAuthentication(String secret, Clock clock, long maxSkewSeconds, ObjectMapper json) {
        if (secret == null || secret.length() < 32 || maxSkewSeconds < 1) {
            throw new IllegalArgumentException("内部调用密钥至少 32 字符，时钟窗口必须为正数");
        }
        this.secret = secret; this.clock = clock; this.maxSkewSeconds = maxSkewSeconds;
        this.json = json.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }
    public <T> AdminBusinessRpc.Call<T> sign(String method, String operator, String permission, String requestId, T payload) {
        var call = new AdminBusinessRpc.Call<>(operator, permission, requestId, clock.instant().getEpochSecond(), payload, "");
        return new AdminBusinessRpc.Call<>(operator, permission, requestId, call.issuedAt(), payload, signature(method, call));
    }
    public void verify(String method, String permission, AdminBusinessRpc.Call<?> call) {
        long now = clock.instant().getEpochSecond();
        if (call == null || call.payload() == null || call.operatorId() == null || call.operatorId().isBlank()
                || call.requestId() == null || call.requestId().isBlank() || !permission.equals(call.permission())
                || call.issuedAt() < now - maxSkewSeconds || call.issuedAt() > now + maxSkewSeconds
                || call.signature() == null || !MessageDigest.isEqual(signature(method, call).getBytes(StandardCharsets.UTF_8),
                        call.signature().getBytes(StandardCharsets.UTF_8))) throw new FafaException(BusinessError.RPC_UNAUTHORIZED);
    }
    private String signature(String method, AdminBusinessRpc.Call<?> call) {
        try {
            byte[] message = json.writeValueAsBytes(new SignedContent(method, call.operatorId(), call.permission(),
                    call.requestId(), call.issuedAt(), call.payload()));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message));
        } catch (java.security.GeneralSecurityException | java.io.IOException exception) {
            throw new IllegalStateException("内部调用签名失败", exception);
        }
    }
    private record SignedContent(String method, String operator, String permission, String requestId, long issuedAt, Object payload) { }
}
