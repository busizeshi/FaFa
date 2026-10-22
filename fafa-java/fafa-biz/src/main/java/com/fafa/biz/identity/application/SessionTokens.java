package com.fafa.biz.identity.application;

public interface SessionTokens {
    record Grant(String token, String tokenType, long expiresIn) {
        @Override public String toString() { return "Grant[REDACTED]"; }
    }
    record Device(String deviceId, String deviceName) { }
    Grant issue(String userId, long authVersion, Device device);
    String requireUserId();
    long currentAuthVersion();
    Device currentDevice();
    boolean hasToken();
    void logout();
    void revokeAll(String userId);
}
