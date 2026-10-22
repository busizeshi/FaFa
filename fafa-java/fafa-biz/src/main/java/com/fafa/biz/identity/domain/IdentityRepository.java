package com.fafa.biz.identity.domain;

import com.fafa.biz.identity.domain.IdentityModels.*;
import java.time.LocalDateTime;
import java.util.List;

/** 持久化边界；带锁方法必须在数据库事务中调用。 */
public interface IdentityRepository {
    Account findByEmail(String email);
    Account findById(String id);
    Account lockAccount(String id);
    Profile profile(String id);
    void insertAccount(Account account, LocalDateTime now);
    void insertProfile(Profile profile);
    void insertConsent(String id, String userId, String purpose, String version, LocalDateTime now);
    List<LegalDocument> publishedDocuments(LocalDateTime now);
    List<LegalDocument> publishedVersion(String type, String version, LocalDateTime now);
    void insertChallenge(Challenge challenge, LocalDateTime now);
    Challenge lockChallenge(String id);
    void invalidateChallenges(String email, String purpose, LocalDateTime now);
    void markChallengeSent(String id, String state);
    void incrementAttempts(String id);
    void consumeChallenge(String id, LocalDateTime now);
    void insertTicket(Ticket ticket);
    Ticket findTicket(String id);
    Ticket lockTicket(String id);
    void consumeTicket(String id, LocalDateTime now);
    void invalidateTickets(String userId, LocalDateTime now);
    void changePassword(String userId, String hash, LocalDateTime now);
    void advanceAuthVersion(String userId, LocalDateTime now);
    void audit(String id, String userId, String action, String result, LocalDateTime now, String requestId);
}
