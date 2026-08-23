package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.UserQuotaUsagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户配额使用记录Mapper
 *
 * @author FaFa Team
 * @since 4.0
 */
@Mapper
public interface UserQuotaUsageMapper extends BaseMapper<UserQuotaUsagePO> {

    /**
     * 存储配额原子增量（检查+扣减单语句，防并发超发）。
     * 返回受影响行数：1=成功，0=配额不足（或行不存在）。
     */
    @Update("UPDATE user_quota_usage SET used_amount = used_amount + #{delta}, updated_at = NOW() "
            + "WHERE user_id = #{userId} AND quota_type = #{quotaType} "
            + "AND used_amount + #{delta} <= #{limit}")
    int increaseWithinLimit(@Param("userId") Long userId, @Param("quotaType") String quotaType,
                            @Param("delta") long delta, @Param("limit") long limit);

    /**
     * 存储配额原子减量（防负数）。
     * 返回受影响行数：1=成功，0=行不存在或会减为负数。
     */
    @Update("UPDATE user_quota_usage SET used_amount = used_amount - #{delta}, updated_at = NOW() "
            + "WHERE user_id = #{userId} AND quota_type = #{quotaType} "
            + "AND used_amount - #{delta} >= 0")
    int decreaseNonNegative(@Param("userId") Long userId, @Param("quotaType") String quotaType,
                            @Param("delta") long delta);
}
