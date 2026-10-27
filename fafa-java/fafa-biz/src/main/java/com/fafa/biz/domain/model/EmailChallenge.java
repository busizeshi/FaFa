package com.fafa.biz.domain.model;

import com.fafa.biz.domain.ChallengePurpose;

import java.time.LocalDateTime;

/**
 * 邮箱验证挑战领域对象（注册验证 / 找回密码）。
 *
 * <p>核心不变量（由 {@link #verify} 保证）：</p>
 * <ul>
 *   <li>已消费的挑战不可再次验证（防重放）；</li>
 *   <li>过期时间由服务端判定，客户端倒计时仅作展示；</li>
 *   <li>尝试次数超限即作废，防暴力枚举验证码。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public class EmailChallenge {

    /**
     * 验证结果枚举：领域层只表达事实，错误码映射交由应用层。
     */
    public enum VerifyOutcome {
        /** 验证成功（挑战已标记消费） */
        SUCCESS,
        /** 挑战不存在 / 已消费 / 已作废（对外统一按 404 处理，防可枚举） */
        UNAVAILABLE,
        /** 已过期 */
        EXPIRED,
        /** 尝试次数超限 */
        EXHAUSTED,
        /** 验证码不匹配（attemptCount 已自增，需持久化） */
        MISMATCH
    }

    /** 数据库主键，对外作为 challengeId */
    private Long id;

    /** 挑战用途 */
    private ChallengePurpose purpose;

    /** 目标邮箱（小写） */
    private String email;

    /** 验证码哈希（bcrypt），明文验证码永不持久化 */
    private String codeHash;

    /** 过期时间（UTC） */
    private LocalDateTime expiresAt;

    /** 已尝试次数 */
    private int attemptCount;

    /** 最大尝试次数 */
    private int maxAttempts;

    /** 消费时间（UTC）；非空表示已使用 */
    private LocalDateTime consumedAt;

    /** 创建时间（UTC） */
    private LocalDateTime createdAt;

    /** 更新时间（UTC） */
    private LocalDateTime updatedAt;

    /**
     * 校验验证码并推进领域状态。
     *
     * <p>调用方在 MISMATCH / SUCCESS 后必须持久化本对象（attemptCount / consumedAt 已变化）。</p>
     *
     * @param code          用户提交的验证码
     * @param hasher        密码哈希端口（由 infrastructure 注入实现）
     * @param now           当前 UTC 时间
     * @return 校验结果
     */
    public VerifyOutcome verify(String code, PasswordHasherPort hasher, LocalDateTime now) {
        // 已消费 / 已作废：统一按不可用处理，不区分细节（防探测）
        if (consumedAt != null) {
            return VerifyOutcome.UNAVAILABLE;
        }
        if (now.isAfter(expiresAt)) {
            return VerifyOutcome.EXPIRED;
        }
        if (attemptCount >= maxAttempts) {
            return VerifyOutcome.EXHAUSTED;
        }
        if (hasher.matches(code, codeHash)) {
            this.consumedAt = now;
            return VerifyOutcome.SUCCESS;
        }
        // 失败：消耗一次尝试机会（应用层负责持久化）
        this.attemptCount++;
        return VerifyOutcome.MISMATCH;
    }

    /**
     * @return 挑战是否仍处于可验证窗口内（用于重发前检查）
     */
    public boolean isConsumed() {
        return consumedAt != null;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChallengePurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ChallengePurpose purpose) {
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
