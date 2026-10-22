package com.fafa.biz.identity.domain;

import java.time.LocalDateTime;

public final class IdentityModels {
    private IdentityModels() { }

    public record Account(String id, String emailCanonical, String emailDisplay,
                          String passwordHash, String status, long authVersion, long version) {
        @Override public String toString() { return "Account[id=" + id + "]"; }
    }
    public record Profile(String userId, String nickname, String signature, String avatarMediaId,
                          String timezone, long version) { }
    public record Challenge(String id, String purpose, String targetCanonical, String codeHmac,
                            LocalDateTime expiresAt, int attempts, int maxAttempts,
                            LocalDateTime consumedAt, String sendState) {
        @Override public String toString() { return "Challenge[id=" + id + "]"; }
    }
    public record Ticket(String id, String userId, String tokenHash, LocalDateTime expiresAt,
                         LocalDateTime consumedAt) {
        @Override public String toString() { return "Ticket[id=" + id + "]"; }
    }
    public record LegalDocument(String type, String version, String title, String body,
                                String contentHash, LocalDateTime effectiveAt) { }
}
