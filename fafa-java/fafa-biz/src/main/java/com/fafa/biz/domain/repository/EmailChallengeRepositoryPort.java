package com.fafa.biz.domain.repository;

import com.fafa.biz.domain.ChallengePurpose;
import com.fafa.biz.domain.model.EmailChallenge;

import java.util.Optional;

/**
 * 邮箱验证挑战仓储端口。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public interface EmailChallengeRepositoryPort {

    /**
     * 新建挑战（验证码哈希已由调用方生成）。
     *
     * @param challenge 领域对象
     * @return 持久化后的对象（含 id）
     */
    EmailChallenge save(EmailChallenge challenge);

    /**
     * @param id challengeId
     * @return 挑战；不存在返回 empty
     */
    Optional<EmailChallenge> findById(Long id);

    /**
     * 查找该邮箱指定用途下最新一张仍可用（未消费、未过期）的挑战。
     *
     * <p>用于幂等重试时复用首次受理的挑战，而不是重复发码或误报"已注册"。</p>
     *
     * @param email   邮箱（小写）
     * @param purpose 挑战用途
     * @return 最新可用挑战；不存在返回 empty
     */
    Optional<EmailChallenge> findLatestUsable(String email, ChallengePurpose purpose);

    /**
     * 更新挑战状态（尝试次数 / 消费标记）。
     *
     * @param challenge 领域对象
     */
    void update(EmailChallenge challenge);
}
