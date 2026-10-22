package com.fafa.biz.identity.application;

public interface CredentialDigests {
    String digest(String namespace, String value);
    boolean matches(String expected, String namespace, String value);
}
