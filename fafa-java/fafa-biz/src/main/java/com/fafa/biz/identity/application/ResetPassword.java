package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.*;
import com.fafa.biz.identity.domain.IdentityModels.*;
import com.fafa.common.error.FafaException;
import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ResetPassword {
    public record TicketView(String resetTicket, Instant expiresAt) {
        @Override public String toString() { return "TicketView[REDACTED]"; }
    }
    private record Verified(TicketView ticket, IdentityError error) { }
    private static final Logger log = LoggerFactory.getLogger(ResetPassword.class);
    private final IdentityRepository repository;
    private final TransactionTemplate tx;
    private final EmailChallenges challenges;
    private final CredentialDigests digests;
    private final PasswordEncoder encoder;
    private final IdentityProperties policy;
    private final SessionTokens sessions;
    private final SecureRandom random;
    private final Clock clock;
    private final SecurityAudit audit;

    public ResetPassword(IdentityRepository repository, TransactionTemplate tx, EmailChallenges challenges,
                         CredentialDigests digests, PasswordEncoder encoder, IdentityProperties policy,
                         SessionTokens sessions, SecureRandom random, Clock clock, SecurityAudit audit) {
        this.repository = repository; this.tx = tx; this.challenges = challenges; this.digests = digests;
        this.encoder = encoder; this.policy = policy; this.sessions = sessions; this.random = random;
        this.clock = clock; this.audit = audit;
    }
    public TicketView verify(String challengeId, String code) {
        Verified result = tx.execute(status -> {
            Challenge challenge = repository.lockChallenge(challengeId);
            IdentityError error = challenges.validateLocked(challenge, ChallengePurpose.RESET_PASSWORD, null, code);
            if (error != null) return new Verified(null, error);
            Account account = repository.findByEmail(challenge.targetCanonical());
            if (account == null || !"ACTIVE".equals(account.status())) {
                return new Verified(null, IdentityError.CHALLENGE_INVALID);
            }
            byte[] secret = new byte[32];
            random.nextBytes(secret);
            String id = UUID.randomUUID().toString();
            String token = id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime expires = now.plusSeconds(policy.resetTicketTtlSeconds());
            repository.insertTicket(new Ticket(id, account.id(), digests.digest("reset", token), expires, null));
            repository.consumeChallenge(challengeId, now);
            return new Verified(new TicketView(token, expires.toInstant(ZoneOffset.UTC)), null);
        });
        if (result == null) throw new IllegalStateException("Reset verification transaction returned no result");
        if (result.error() != null) throw new FafaException(result.error());
        return result.ticket();
    }
    public void reset(String token, String password, String confirmation) {
        PasswordPolicy.validate(password, confirmation);
        String id = tokenId(token);
        Ticket lookup = repository.findTicket(id);
        if (lookup == null) throw new FafaException(IdentityError.TICKET_INVALID);
        String hash = encoder.encode(password);
        tx.executeWithoutResult(status -> {
            Account account = repository.lockAccount(lookup.userId());
            Ticket ticket = repository.lockTicket(id);
            LocalDateTime now = LocalDateTime.now(clock);
            if (account == null || !"ACTIVE".equals(account.status()) || ticket == null
                    || ticket.consumedAt() != null || !now.isBefore(ticket.expiresAt())
                    || !digests.matches(ticket.tokenHash(), "reset", token)) {
                throw new FafaException(IdentityError.TICKET_INVALID);
            }
            repository.changePassword(account.id(), hash, now);
            repository.invalidateTickets(account.id(), now);
            audit.record(account.id(), "RESET_PASSWORD", "SUCCEEDED");
        });
        try {
            sessions.revokeAll(lookup.userId());
        } catch (org.springframework.dao.DataAccessException failure) {
            // authVersion 已提交；即使 Redis 清理不可用，旧 Token 也会被拒绝。
            log.warn("Session cleanup deferred after password reset");
        }
    }
    private String tokenId(String token) {
        if (token == null || !token.matches("[a-f0-9-]{36}\\.[A-Za-z0-9_-]{43}")) {
            throw new FafaException(IdentityError.TICKET_INVALID);
        }
        return token.substring(0, 36);
    }
}
