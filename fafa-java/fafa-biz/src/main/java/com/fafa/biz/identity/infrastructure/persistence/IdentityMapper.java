package com.fafa.biz.identity.infrastructure.persistence;

import com.fafa.biz.identity.domain.IdentityModels.*;
import com.fafa.biz.identity.domain.IdentityRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface IdentityMapper extends IdentityRepository {
    @Select("SELECT * FROM user_account WHERE email_canonical=#{email}")
    Account findByEmail(String email);

    @Select("SELECT * FROM user_account WHERE id=#{id}")
    Account findById(String id);

    @Select("SELECT * FROM user_account WHERE id=#{id} FOR UPDATE")
    Account lockAccount(String id);

    @Select("SELECT * FROM user_profile WHERE user_id=#{id}")
    Profile profile(String id);

    @Insert("""
        INSERT INTO user_account(id,email_canonical,email_display,password_hash,status,auth_version,version,created_at,updated_at)
        VALUES(#{a.id},#{a.emailCanonical},#{a.emailDisplay},#{a.passwordHash},#{a.status},#{a.authVersion},#{a.version},#{now},#{now})
        """)
    void insertAccount(@Param("a") Account account, @Param("now") LocalDateTime now);

    @Insert("""
        INSERT INTO user_profile(user_id,nickname,signature,avatar_media_id,timezone,version)
        VALUES(#{userId},#{nickname},#{signature},#{avatarMediaId},#{timezone},#{version})
        """)
    void insertProfile(Profile profile);

    @Insert("""
        INSERT INTO user_consent(id,user_id,purpose,document_version,accepted_at)
        VALUES(#{id},#{userId},#{purpose},#{version},#{now})
        """)
    void insertConsent(@Param("id") String id, @Param("userId") String userId,
                       @Param("purpose") String purpose, @Param("version") String version,
                       @Param("now") LocalDateTime now);

    @Select("""
        SELECT d.document_type AS type,d.version,d.title,d.body,d.content_hash,d.effective_at
        FROM legal_document d WHERE d.status='PUBLISHED' AND d.effective_at<=#{now}
        ORDER BY d.document_type,d.effective_at DESC,d.version DESC
        """)
    List<LegalDocument> publishedDocuments(LocalDateTime now);

    @Select("""
        SELECT document_type AS type,version,title,body,content_hash,effective_at
        FROM legal_document WHERE document_type=#{type} AND version=#{version}
        AND status='PUBLISHED' AND effective_at<=#{now}
        """)
    List<LegalDocument> publishedVersion(@Param("type") String type, @Param("version") String version,
                                         @Param("now") LocalDateTime now);

    @Insert("""
        INSERT INTO auth_challenge(id,purpose,target_canonical,code_hmac,expires_at,attempts,max_attempts,send_state,created_at)
        VALUES(#{c.id},#{c.purpose},#{c.targetCanonical},#{c.codeHmac},#{c.expiresAt},0,#{c.maxAttempts},#{c.sendState},#{now})
        """)
    void insertChallenge(@Param("c") Challenge challenge, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM auth_challenge WHERE id=#{id} FOR UPDATE")
    Challenge lockChallenge(String id);

    @Update("""
        UPDATE auth_challenge SET consumed_at=#{now},version=version+1
        WHERE target_canonical=#{email} AND purpose=#{purpose} AND consumed_at IS NULL
        """)
    void invalidateChallenges(@Param("email") String email, @Param("purpose") String purpose,
                              @Param("now") LocalDateTime now);

    @Update("UPDATE auth_challenge SET send_state=#{state},version=version+1 WHERE id=#{id}")
    void markChallengeSent(@Param("id") String id, @Param("state") String state);

    @Update("UPDATE auth_challenge SET attempts=attempts+1,version=version+1 WHERE id=#{id}")
    void incrementAttempts(String id);

    @Update("UPDATE auth_challenge SET consumed_at=#{now},version=version+1 WHERE id=#{id}")
    void consumeChallenge(@Param("id") String id, @Param("now") LocalDateTime now);

    @Insert("""
        INSERT INTO auth_ticket(id,user_id,token_hash,purpose,expires_at)
        VALUES(#{id},#{userId},#{tokenHash},'RESET_PASSWORD',#{expiresAt})
        """)
    void insertTicket(Ticket ticket);

    @Select("SELECT id,user_id,token_hash,expires_at,consumed_at FROM auth_ticket WHERE id=#{id} AND purpose='RESET_PASSWORD'")
    Ticket findTicket(String id);

    @Select("SELECT id,user_id,token_hash,expires_at,consumed_at FROM auth_ticket WHERE id=#{id} AND purpose='RESET_PASSWORD' FOR UPDATE")
    Ticket lockTicket(String id);

    @Update("UPDATE auth_ticket SET consumed_at=#{now},version=version+1 WHERE id=#{id}")
    void consumeTicket(@Param("id") String id, @Param("now") LocalDateTime now);

    @Update("UPDATE auth_ticket SET consumed_at=#{now},version=version+1 WHERE user_id=#{userId} AND consumed_at IS NULL")
    void invalidateTickets(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Update("UPDATE user_account SET password_hash=#{hash},auth_version=auth_version+1,version=version+1,updated_at=#{now} WHERE id=#{userId}")
    void changePassword(@Param("userId") String userId, @Param("hash") String hash, @Param("now") LocalDateTime now);

    @Update("UPDATE user_account SET auth_version=auth_version+1,version=version+1,updated_at=#{now} WHERE id=#{userId}")
    void advanceAuthVersion(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Insert("""
        INSERT INTO security_audit(event_id,actor_id,action,result,occurred_at,request_id)
        VALUES(#{id},#{userId},#{action},#{result},#{now},#{requestId})
        """)
    void audit(@Param("id") String id, @Param("userId") String userId, @Param("action") String action,
               @Param("result") String result, @Param("now") LocalDateTime now, @Param("requestId") String requestId);
}
