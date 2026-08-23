package com.fafa.application.service;

import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserQuotaUsageRepository;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 配额原子边界测试。 */
class QuotaApplicationServiceTest {

    private UserQuotaUsageRepository quotaUsageRepository;
    private UserRepository userRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private FaFaProperties properties;
    private QuotaApplicationService service;

    @BeforeEach
    void setUp() {
        quotaUsageRepository = mock(UserQuotaUsageRepository.class);
        userRepository = mock(UserRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        properties = new FaFaProperties();
        properties.getQuota().getStorage().setNormalBytes(100L);
        properties.getQuota().getAiSearch().setMonthlyNormal(10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        User user = mock(User.class);
        when(user.isVipValid()).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        service = new QuotaApplicationService(quotaUsageRepository, userRepository, redisTemplate, properties);
    }

    @Test
    @DisplayName("首次上传单文件超过配额时直接拒绝，不插入超额用量")
    void increaseStorage_shouldRejectDeltaLargerThanLimit() {
        assertThatThrownBy(() -> service.increaseStorage(1L, 101L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        verify(quotaUsageRepository, never()).initIfAbsent(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("原子增量失败时抛出存储配额异常")
    void increaseStorage_shouldRejectWhenAtomicUpdateFails() {
        when(quotaUsageRepository.getUsedAmount(1L, "STORAGE_USED")).thenReturn(50L);
        when(quotaUsageRepository.increaseWithinLimit(1L, "STORAGE_USED", 51L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> service.increaseStorage(1L, 51L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORAGE_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("AI 检索第11次超限时回滚 Redis 计数")
    void tryAcquireAiSearch_shouldRollbackWhenOverLimit() {
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThatThrownBy(() -> service.tryAcquireAiSearch(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_SEARCH_QUOTA_EXCEEDED);
        verify(valueOperations).decrement(anyString());
    }
}
