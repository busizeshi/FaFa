package com.fafa.application.service;

import com.fafa.application.dto.quota.QuotaDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserQuotaUsageRepository;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * 配额应用服务
 *
 * 负责会员配额机制（见 phase4-implementation-plan.md 第 9 节）：
 * - 存储配额：user_quota_usage 表条件 UPDATE 原子扣减/释放（防并发超发）
 * - AI 语义检索月配额：Redis 月 key INCR/DECR（失败请求不占额度）
 * - 配额与用量查询（「我的」页进度条）
 *
 * 会员身份读取复用 User.isVipValid()（过期检查由 UserApplicationService 已处理）。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaApplicationService {

    /** 存储配额类型（user_quota_usage.quota_type） */
    private static final String QUOTA_TYPE_STORAGE = "STORAGE_USED";
    /** AI 检索月配额 Redis key 前缀 */
    private static final String AI_SEARCH_KEY_PREFIX = "fafa:quota:ai-search:";
    /** 食品包装识别月配额 Redis key 前缀 */
    private static final String FOOD_RECOGNITION_KEY_PREFIX = "fafa:quota:food-recognition:";
    private static final String REPORT_CONTENT_KEY_PREFIX = "fafa:quota:report-content:";

    private final UserQuotaUsageRepository quotaUsageRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final FaFaProperties faFaProperties;

    /**
     * 存储配额原子增量（上传前校验+扣减）
     *
     * @throws BusinessException STORAGE_QUOTA_EXCEEDED 配额不足
     */
    public void increaseStorage(Long userId, long deltaBytes) {
        if (deltaBytes <= 0) {
            return;
        }
        long limit = storageLimit(userId);
        if (deltaBytes > limit) {
            throw new BusinessException(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        }

        // 行不存在时先初始化（并发首写冲突则退回 UPDATE 路径）
        if (quotaUsageRepository.getUsedAmount(userId, QUOTA_TYPE_STORAGE) == 0
                && quotaUsageRepository.initIfAbsent(userId, QUOTA_TYPE_STORAGE, deltaBytes)) {
            return;
        }

        boolean ok = quotaUsageRepository.increaseWithinLimit(userId, QUOTA_TYPE_STORAGE, deltaBytes, limit);
        if (!ok) {
            log.info("存储配额不足: userId={}, delta={}, limit={}", userId, deltaBytes, limit);
            throw new BusinessException(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        }
    }

    /**
     * 存储配额原子减量（删除素材后释放）
     */
    public void decreaseStorage(Long userId, long deltaBytes) {
        if (deltaBytes <= 0) {
            return;
        }
        boolean ok = quotaUsageRepository.decreaseNonNegative(userId, QUOTA_TYPE_STORAGE, deltaBytes);
        if (!ok) {
            // 行不存在或会减为负数：记日志不阻断（删除主流程已成功）
            log.warn("存储配额释放失败（行不存在或会减为负）: userId={}, delta={}", userId, deltaBytes);
        }
    }

    /**
     * AI 语义检索配额获取（普通用户月限额，VIP 不限）
     *
     * 原子不超发：INCR 后超限则 DECR 回滚。
     *
     * @throws BusinessException AI_SEARCH_QUOTA_EXCEEDED 本月次数已用完
     */
    public void tryAcquireAiSearch(Long userId) {
        if (isVip(userId)) {
            return;
        }
        int limit = faFaProperties.getQuota().getAiSearch().getMonthlyNormal();
        String key = aiSearchKey(userId);

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次计数设置 TTL 至当月末
            stringRedisTemplate.expire(key, ttlToEndOfMonth());
        }
        if (count != null && count > limit) {
            stringRedisTemplate.opsForValue().decrement(key);
            log.info("AI 语义检索月配额已用完: userId={}, limit={}", userId, limit);
            throw new BusinessException(ErrorCode.AI_SEARCH_QUOTA_EXCEEDED);
        }
    }

    /**
     * AI 语义检索配额回滚（网关调用失败，失败请求不占额度）
     */
    public void releaseAiSearch(Long userId) {
        if (isVip(userId)) {
            return;
        }
        stringRedisTemplate.opsForValue().decrement(aiSearchKey(userId));
    }

    /** 获取食品包装识别月配额，普通用户和 VIP 使用独立配置上限。 */
    public void tryAcquireFoodRecognition(Long userId) {
        int limit = foodRecognitionLimit(userId);
        String key = foodRecognitionKey(userId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, ttlToEndOfMonth());
        }
        if (count != null && count > limit) {
            stringRedisTemplate.opsForValue().decrement(key);
            log.info("食品包装识别月配额已用完: userId={}, limit={}", userId, limit);
            throw new BusinessException(ErrorCode.FOOD_RECOGNITION_QUOTA_EXCEEDED);
        }
    }

    /** 识别服务失败时释放本次已获取的配额。 */
    public void releaseFoodRecognition(Long userId) {
        String key = foodRecognitionKey(userId);
        Long count = stringRedisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            stringRedisTemplate.opsForValue().set(key, "0");
        }
    }

    /** 报告与内容共用月配额，防止两个入口分别绕过限制。 */
    public void tryAcquireReportContent(Long userId) {
        int limit = reportContentLimit(userId);
        String key = reportContentKey(userId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, ttlToEndOfMonth());
        }
        if (count != null && count > limit) {
            stringRedisTemplate.opsForValue().decrement(key);
            throw new BusinessException(ErrorCode.REPORT_GENERATION_QUOTA_EXCEEDED);
        }
    }

    public void releaseReportContent(Long userId) {
        Long count = stringRedisTemplate.opsForValue().decrement(reportContentKey(userId));
        if (count != null && count < 0) {
            stringRedisTemplate.opsForValue().set(reportContentKey(userId), "0");
        }
    }

    /**
     * 配额与用量查询
     */
    public QuotaDTO getQuota(Long userId) {
        boolean vip = isVip(userId);
        long storageUsed = quotaUsageRepository.getUsedAmount(userId, QUOTA_TYPE_STORAGE);
        long storageLimit = storageLimit(userId);

        long aiSearchUsed = 0;
        long aiSearchLimit = -1;
        if (!vip) {
            String used = stringRedisTemplate.opsForValue().get(aiSearchKey(userId));
            aiSearchUsed = used == null ? 0 : Long.parseLong(used);
            aiSearchLimit = faFaProperties.getQuota().getAiSearch().getMonthlyNormal();
        }

        String foodUsedValue = stringRedisTemplate.opsForValue().get(foodRecognitionKey(userId));
        long foodRecognitionUsed = foodUsedValue == null ? 0 : Long.parseLong(foodUsedValue);
        long foodRecognitionLimit = vip ? -1 : faFaProperties.getQuota().getFoodRecognition().getMonthlyNormal();
        String reportUsedValue = stringRedisTemplate.opsForValue().get(reportContentKey(userId));
        long reportContentUsed = reportUsedValue == null ? 0 : Long.parseLong(reportUsedValue);
        long reportContentLimit = vip ? faFaProperties.getQuota().getReportContent().getMonthlyVip()
                : faFaProperties.getQuota().getReportContent().getMonthlyNormal();
        return new QuotaDTO(storageUsed, storageLimit, aiSearchUsed, aiSearchLimit,
                foodRecognitionUsed, foodRecognitionLimit, vip, reportContentUsed, reportContentLimit);
    }

    /**
     * 存储上限（按会员身份）
     */
    private long storageLimit(Long userId) {
        return isVip(userId)
                ? faFaProperties.getQuota().getStorage().getVipBytes()
                : faFaProperties.getQuota().getStorage().getNormalBytes();
    }

    private boolean isVip(Long userId) {
        return userRepository.findById(userId)
                .map(User::isVipValid)
                .orElse(false);
    }

    private String aiSearchKey(Long userId) {
        return AI_SEARCH_KEY_PREFIX + userId + ":" + YearMonth.now();
    }

    private String foodRecognitionKey(Long userId) {
        return FOOD_RECOGNITION_KEY_PREFIX + userId + ":" + YearMonth.now();
    }

    private int foodRecognitionLimit(Long userId) {
        return isVip(userId)
                ? faFaProperties.getQuota().getFoodRecognition().getMonthlyVip()
                : faFaProperties.getQuota().getFoodRecognition().getMonthlyNormal();
    }

    private String reportContentKey(Long userId) {
        return REPORT_CONTENT_KEY_PREFIX + userId + ":" + YearMonth.now();
    }

    private int reportContentLimit(Long userId) {
        return isVip(userId) ? faFaProperties.getQuota().getReportContent().getMonthlyVip()
                : faFaProperties.getQuota().getReportContent().getMonthlyNormal();
    }

    private Duration ttlToEndOfMonth() {
        LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
                .plusDays(1).atStartOfDay();
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endOfMonth);
        return Duration.ofSeconds(Math.max(seconds, 1));
    }
}
