package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.repository.UserQuotaUsageRepository;
import com.fafa.infrastructure.persistence.mapper.UserQuotaUsageMapper;
import com.fafa.infrastructure.persistence.po.UserQuotaUsagePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 用户配额使用记录仓储实现
 *
 * @author FaFa Team
 * @since 4.0
 */
@Repository
@RequiredArgsConstructor
public class UserQuotaUsageRepositoryImpl implements UserQuotaUsageRepository {

    private final UserQuotaUsageMapper userQuotaUsageMapper;

    @Override
    public long getUsedAmount(Long userId, String quotaType) {
        LambdaQueryWrapper<UserQuotaUsagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserQuotaUsagePO::getUserId, userId)
               .eq(UserQuotaUsagePO::getQuotaType, quotaType);
        UserQuotaUsagePO po = userQuotaUsageMapper.selectOne(wrapper);
        return po == null || po.getUsedAmount() == null ? 0L : po.getUsedAmount();
    }

    @Override
    public boolean increaseWithinLimit(Long userId, String quotaType, long delta, long limit) {
        return userQuotaUsageMapper.increaseWithinLimit(userId, quotaType, delta, limit) > 0;
    }

    @Override
    public boolean decreaseNonNegative(Long userId, String quotaType, long delta) {
        return userQuotaUsageMapper.decreaseNonNegative(userId, quotaType, delta) > 0;
    }

    @Override
    public boolean initIfAbsent(Long userId, String quotaType, long delta) {
        UserQuotaUsagePO po = new UserQuotaUsagePO();
        po.setUserId(userId);
        po.setQuotaType(quotaType);
        po.setUsedAmount(delta);
        try {
            userQuotaUsageMapper.insert(po);
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发首写：行已存在，交由调用方重试 UPDATE 路径
            return false;
        }
    }
}
