package com.fafa.biz.identity.infrastructure;

import com.fafa.biz.identity.application.IdentityProperties;
import com.fafa.biz.identity.application.CredentialDigests;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SecretDigests implements CredentialDigests {
    private final byte[] secret;
    public SecretDigests(IdentityProperties properties) {
        secret = properties.hmacSecret().getBytes(StandardCharsets.UTF_8);
    }
    public String digest(String namespace, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((namespace + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC is unavailable", exception);
        }
    }
    public boolean matches(String expected, String namespace, String value) {
        if (expected == null || value == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                digest(namespace, value).getBytes(StandardCharsets.US_ASCII));
    }
}
