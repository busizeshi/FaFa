package com.fafa.application.port;

public interface InvitationTokenGenerator {
    String generate();
    String sha256(String rawToken);
}
