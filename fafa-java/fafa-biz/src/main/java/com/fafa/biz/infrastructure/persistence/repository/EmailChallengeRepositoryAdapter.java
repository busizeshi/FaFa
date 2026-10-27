package com.fafa.biz.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.biz.domain.ChallengePurpose;
import com.fafa.biz.domain.model.EmailChallenge;
import com.fafa.biz.domain.repository.EmailChallengeRepositoryPort;
import com.fafa.biz.infrastructure.persistence.entity.EmailChallengeDO;
import com.fafa.biz.infrastructure.persistence.mapper.EmailChallengeMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * 邮箱验证挑战仓储适配器。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class EmailChallengeRepositoryAdapter implements EmailChallengeRepositoryPort {

    private final EmailChallengeMapper challengeMapper;

    /**
     * @param challengeMapper MyBatis-Plus Mapper
     */
    public EmailChallengeRepositoryAdapter(EmailChallengeMapper challengeMapper) {
        this.challengeMapper = challengeMapper;
    }

    /** {@inheritDoc} */
    @Override
    public EmailChallenge save(EmailChallenge challenge) {
        EmailChallengeDO challengeDO = toDO(challenge);
        if (challengeDO.getId() == null) {
            challengeMapper.insert(challengeDO);
        } else {
            challengeMapper.updateById(challengeDO);
        }
        return toDomain(challengeDO);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<EmailChallenge> findById(Long id) {
        return Optional.ofNullable(challengeMapper.selectById(id)).map(this::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<EmailChallenge> findLatestUsable(String email, ChallengePurpose purpose) {
        LambdaQueryWrapper<EmailChallengeDO> wrapper = new LambdaQueryWrapper<EmailChallengeDO>()
                .eq(EmailChallengeDO::getEmail, email)
                .eq(EmailChallengeDO::getPurpose, purpose.name())
                .isNull(EmailChallengeDO::getConsumedAt)
                .gt(EmailChallengeDO::getExpiresAt, LocalDateTime.now(ZoneOffset.UTC))
                .orderByDesc(EmailChallengeDO::getId)
                .last("LIMIT 1");
        return Optional.ofNullable(challengeMapper.selectOne(wrapper)).map(this::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public void update(EmailChallenge challenge) {
        challengeMapper.updateById(toDO(challenge));
    }

    /**
     * 领域对象 → DO。
     */
    private EmailChallengeDO toDO(EmailChallenge challenge) {
        EmailChallengeDO challengeDO = new EmailChallengeDO();
        challengeDO.setId(challenge.getId());
        challengeDO.setPurpose(challenge.getPurpose() == null ? null : challenge.getPurpose().name());
        challengeDO.setEmail(challenge.getEmail());
        challengeDO.setCodeHash(challenge.getCodeHash());
        challengeDO.setExpiresAt(challenge.getExpiresAt());
        challengeDO.setAttemptCount(challenge.getAttemptCount());
        challengeDO.setMaxAttempts(challenge.getMaxAttempts());
        challengeDO.setConsumedAt(challenge.getConsumedAt());
        challengeDO.setCreatedAt(challenge.getCreatedAt());
        challengeDO.setUpdatedAt(challenge.getUpdatedAt());
        return challengeDO;
    }

    /**
     * DO → 领域对象。
     */
    private EmailChallenge toDomain(EmailChallengeDO challengeDO) {
        EmailChallenge challenge = new EmailChallenge();
        challenge.setId(challengeDO.getId());
        challenge.setPurpose(ChallengePurpose.valueOf(challengeDO.getPurpose()));
        challenge.setEmail(challengeDO.getEmail());
        challenge.setCodeHash(challengeDO.getCodeHash());
        challenge.setExpiresAt(challengeDO.getExpiresAt());
        challenge.setAttemptCount(challengeDO.getAttemptCount());
        challenge.setMaxAttempts(challengeDO.getMaxAttempts());
        challenge.setConsumedAt(challengeDO.getConsumedAt());
        challenge.setCreatedAt(challengeDO.getCreatedAt());
        challenge.setUpdatedAt(challengeDO.getUpdatedAt());
        return challenge;
    }
}
