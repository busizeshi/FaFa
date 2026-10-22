package com.fafa.biz.identity.interfaces.http;

import com.fafa.biz.identity.application.SessionTokens;
import com.fafa.biz.identity.domain.ChallengePurpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;

public final class IdentityRequests {
    private IdentityRequests() { }
    public record EmailChallenge(@NotNull ChallengePurpose purpose, @Email @NotBlank @Size(max = 254) String email) {
        public EmailChallenge { if (email != null) email = email.strip(); }
    }
    public record Device(@NotBlank @Size(max = 100) String deviceId,
                         @NotBlank @Size(max = 100) String deviceName) {
        public SessionTokens.Device toModel() { return new SessionTokens.Device(deviceId, deviceName); }
    }
    public record Registration(@Email @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 256) String password, @NotBlank @Size(max = 256) String confirmedPassword,
            @NotBlank @Size(max = 36) String challengeId, @Pattern(regexp = "[0-9]{6}") @NotNull String code,
            @NotNull @Size(min = 2, max = 2) Map<@NotBlank @Size(max = 32) String, @NotBlank @Size(max = 64) String> legalVersions,
            @NotNull @Valid Device device) {
        public Registration { if (email != null) email = email.strip(); }
        @Override public String toString() { return "Registration[REDACTED]"; }
    }
    public record Login(@Email @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 256) String password, @NotNull @Valid Device device) {
        public Login { if (email != null) email = email.strip(); }
        @Override public String toString() { return "Login[REDACTED]"; }
    }
    public record VerifyReset(@NotBlank @Size(max = 36) String challengeId,
                              @Pattern(regexp = "[0-9]{6}") @NotNull String code) {
        @Override public String toString() { return "VerifyReset[REDACTED]"; }
    }
    public record PasswordReset(@NotBlank @Size(max = 128) String resetTicket,
            @NotBlank @Size(max = 256) String password, @NotBlank @Size(max = 256) String confirmedPassword) {
        @Override public String toString() { return "PasswordReset[REDACTED]"; }
    }
}
