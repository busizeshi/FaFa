package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.*;
import com.fafa.biz.identity.domain.IdentityModels.*;
import com.fafa.common.error.FafaException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RegisterAccount {
    private record Outcome(String userId, IdentityError error) { }
    private final IdentityRepository repository;
    private final TransactionTemplate tx;
    private final EmailChallenges challenges;
    private final LegalDocuments legal;
    private final PasswordEncoder encoder;
    private final IdentityProperties policy;
    private final SecureRandom random;
    private final Clock clock;
    private final SecurityAudit audit;

    public RegisterAccount(IdentityRepository repository, TransactionTemplate tx, EmailChallenges challenges,
                           LegalDocuments legal, PasswordEncoder encoder, IdentityProperties policy,
                           SecureRandom random, Clock clock, SecurityAudit audit) {
        this.repository = repository; this.tx = tx; this.challenges = challenges; this.legal = legal;
        this.encoder = encoder; this.policy = policy; this.random = random; this.clock = clock; this.audit = audit;
    }
    public String register(String rawEmail, String password, String confirmation, String challengeId,
                           String code, Map<String, String> versions) {
        PasswordPolicy.validate(password, confirmation);
        EmailAddress email = EmailAddress.parse(rawEmail);
        legal.requireAccepted(versions);
        String hash = encoder.encode(password);
        Outcome outcome;
        try {
            outcome = tx.execute(status -> registerLocked(email, hash, challengeId, code, versions));
        } catch (DuplicateKeyException conflict) {
            throw new FafaException(IdentityError.EMAIL_REGISTERED);
        }
        if (outcome == null) throw new IllegalStateException("Registration transaction returned no result");
        if (outcome.error() != null) throw new FafaException(outcome.error());
        return outcome.userId();
    }
    private Outcome registerLocked(EmailAddress email, String hash, String challengeId, String code,
                                   Map<String, String> versions) {
        Challenge challenge = repository.lockChallenge(challengeId);
        IdentityError error = challenges.validateLocked(challenge, ChallengePurpose.REGISTER, email.canonical(), code);
        if (error != null) return new Outcome(null, error);
        legal.requireAccepted(versions);
        if (repository.findByEmail(email.canonical()) != null) return new Outcome(null, IdentityError.EMAIL_REGISTERED);
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        repository.insertAccount(new Account(id, email.canonical(), email.display(), hash, "ACTIVE", 1, 1), now);
        repository.insertProfile(new Profile(id, "铲屎官" + randomSuffix(), null, null, policy.defaultTimezone(), 1));
        versions.forEach((purpose, version) ->
                repository.insertConsent(UUID.randomUUID().toString(), id, purpose, version, now));
        repository.consumeChallenge(challengeId, now);
        audit.record(id, "REGISTER", "SUCCEEDED");
        return new Outcome(id, null);
    }
    private String randomSuffix() {
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder suffix = new StringBuilder(4);
        for (int i = 0; i < 4; i++) suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return suffix.toString();
    }
}
