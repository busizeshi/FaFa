package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.*;
import com.fafa.biz.identity.domain.IdentityModels.Challenge;
import com.fafa.common.error.FafaException;
import java.security.SecureRandom;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailChallenges {
    public record Receipt(String challengeId, Instant expiresAt, int resendAfter) { }
    private final IdentityRepository repository;
    private final TransactionTemplate tx;
    private final IdentityProperties policy;
    private final AuthRateLimiter limiter;
    private final VerificationMailSender mail;
    private final CredentialDigests digests;
    private final SecureRandom random;
    private final Clock clock;

    public EmailChallenges(IdentityRepository repository, TransactionTemplate tx, IdentityProperties policy,
                           AuthRateLimiter limiter, VerificationMailSender mail, CredentialDigests digests,
                           SecureRandom random, Clock clock) {
        this.repository = repository; this.tx = tx; this.policy = policy; this.limiter = limiter;
        this.mail = mail; this.digests = digests; this.random = random; this.clock = clock;
    }
    public Receipt create(ChallengePurpose purpose, String rawEmail, String address) {
        EmailAddress email = EmailAddress.parse(rawEmail);
        limiter.challenge(email.canonical(), address, purpose);
        String id = UUID.randomUUID().toString();
        String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(clock);
        var challenge = new Challenge(id, purpose.name(), email.canonical(),
                digests.digest(id, code), now.plusSeconds(policy.challengeTtlSeconds()),
                0, policy.maxAttempts(), null, "PENDING");
        tx.executeWithoutResult(status -> {
            repository.invalidateChallenges(email.canonical(), purpose.name(), now);
            repository.insertChallenge(challenge, now);
        });
        send(purpose, email, challenge, code);
        return new Receipt(id, challenge.expiresAt().toInstant(ZoneOffset.UTC), policy.resendSeconds());
    }
    private void send(ChallengePurpose purpose, EmailAddress email, Challenge challenge, String code) {
        boolean eligible = purpose != ChallengePurpose.RESET_PASSWORD
                || repository.findByEmail(email.canonical()) != null;
        if (!eligible) {
            repository.markChallengeSent(challenge.id(), "SUPPRESSED");
            return;
        }
        try {
            mail.send(email.display(), code, (policy.challengeTtlSeconds() + 59) / 60);
            repository.markChallengeSent(challenge.id(), "SENT");
        } catch (org.springframework.mail.MailException failure) {
            repository.markChallengeSent(challenge.id(), "FAILED");
            if (purpose == ChallengePurpose.REGISTER) throw new FafaException(IdentityError.MAIL_UNAVAILABLE);
        }
    }
    /** 返回错误而不是直接抛出，使失败次数更新可以提交。 */
    public IdentityError validateLocked(Challenge challenge, ChallengePurpose purpose, String email, String code) {
        if (challenge == null || challenge.consumedAt() != null
                || !challenge.purpose().equals(purpose.name())
                || (email != null && !challenge.targetCanonical().equals(email))
                || !"SENT".equals(challenge.sendState()) || challenge.attempts() >= challenge.maxAttempts()) {
            return IdentityError.CHALLENGE_INVALID;
        }
        if (!LocalDateTime.now(clock).isBefore(challenge.expiresAt())) return IdentityError.CHALLENGE_EXPIRED;
        if (!digests.matches(challenge.codeHmac(), challenge.id(), code)) {
            repository.incrementAttempts(challenge.id());
            return IdentityError.CHALLENGE_INVALID;
        }
        return null;
    }
}
