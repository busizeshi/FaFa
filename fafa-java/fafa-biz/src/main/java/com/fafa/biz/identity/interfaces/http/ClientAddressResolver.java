package com.fafa.biz.identity.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientAddressResolver {
    private final Set<String> trustedProxies;
    public ClientAddressResolver(@Value("${fafa.identity.trusted-proxy-addresses:}") Set<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }
    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String candidate = request.getHeader("X-Fafa-Client-IP");
        if (trustedProxies.contains(peer) && candidate != null && candidate.length() <= 45
                && candidate.matches("[0-9a-fA-F:.]+")) {
            return candidate;
        }
        return peer;
    }
}
