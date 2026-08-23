package com.fafa.domain.repository;

/**
 * 用户配额使用记录仓储接口
 *
 * 纯计量数据（无状态机、无不变式），不建领域聚合，仓储直接暴露原子操作。
 *
 * @author FaFa Team
 * @since 4.0
 */
public interface UserQuotaUsageRepository {

    /**
     * 查询已使用量（行不存在返回 0）
     */
    long getUsedAmount(Long userId, String quotaType);

    /**
     * 原子增量（检查+扣减单语句，防并发超发）
     *
     * @return true=成功；false=配额不足或行不存在
     */
    boolean increaseWithinLimit(Long userId, String quotaType, long delta, long limit);

    /**
     * 原子减量（防负数）
     *
     * @return true=成功；false=行不存在或会减为负数
     */
    boolean decreaseNonNegative(Long userId, String quotaType, long delta);

    /**
     * 行不存在时初始化（used_amount=delta）；已存在则不覆盖
     *
     * @return true=本次插入；false=行已存在
     */
    boolean initIfAbsent(Long userId, String quotaType, long delta);
}
