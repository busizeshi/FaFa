package com.fafa.biz.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.biz.infrastructure.persistence.entity.IdempotencyRecordDO;
import com.fafa.biz.infrastructure.persistence.mapper.IdempotencyRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 写操作幂等服务（架构规范 §9.1 / 手册 §6.2）。
 *
 * <p>用法：应用服务在处理写命令前调用 {@link #tryBegin}：
 * 返回 false 表示同一 (用户, 操作域, 幂等键) 已处理过，本次应直接返回首次结果或提示重复，
 * 不得重复执行副作用。首次处理成功后调用 {@link #complete} 记录结果码。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    /**
     * @param idempotencyRecordMapper 幂等记录 Mapper
     */
    public IdempotencyService(IdempotencyRecordMapper idempotencyRecordMapper) {
        this.idempotencyRecordMapper = idempotencyRecordMapper;
    }

    /**
     * 尝试占用幂等键（唯一约束兜底并发重复提交）。
     *
     * @param userId 用户 ID（未登录场景传 0）
     * @param scope  操作域，如 auth.register
     * @param key    客户端 Idempotency-Key
     * @return true=首次处理；false=重复请求
     */
    public boolean tryBegin(Long userId, String scope, String key) {
        IdempotencyRecordDO record = new IdempotencyRecordDO();
        record.setUserId(userId == null ? 0L : userId);
        record.setScope(scope);
        record.setIdempotencyKey(key);
        try {
            idempotencyRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("[幂等] 重复请求被拦截 userId={} scope={} key={}", userId, scope, key);
            return false;
        }
    }

    /**
     * 首次处理完成后回填结果码（可选，便于排查与后续响应快照扩展）。
     *
     * <p>注意：{@code userId} 必须与 {@link #tryBegin} 传入的值一致，否则 WHERE 匹配不到记录。</p>
     *
     * @param userId 用户 ID（需与 tryBegin 相同；未登录场景传 0）
     * @param scope  操作域
     * @param key    幂等键
     * @param code   结果码
     */
    public void complete(Long userId, String scope, String key, String code) {
        LambdaQueryWrapper<IdempotencyRecordDO> wrapper = new LambdaQueryWrapper<IdempotencyRecordDO>()
                .eq(IdempotencyRecordDO::getUserId, userId == null ? 0L : userId)
                .eq(IdempotencyRecordDO::getScope, scope)
                .eq(IdempotencyRecordDO::getIdempotencyKey, key);
        IdempotencyRecordDO patch = new IdempotencyRecordDO();
        patch.setResponseCode(code);
        idempotencyRecordMapper.update(patch, wrapper);
    }

    /**
     * 释放幂等键：本次提交未产生任何副作用时调用，允许客户端用同一 key 重新提交。
     *
     * <p>若不释放，一旦占位后处理失败，该 key 会永久停留在"已处理"状态，
     * 客户端重试只会被判定为重复请求而无法继续。</p>
     *
     * @param userId 用户 ID（需与 tryBegin 相同；未登录场景传 0）
     * @param scope  操作域
     * @param key    幂等键
     */
    public void release(Long userId, String scope, String key) {
        LambdaQueryWrapper<IdempotencyRecordDO> wrapper = new LambdaQueryWrapper<IdempotencyRecordDO>()
                .eq(IdempotencyRecordDO::getUserId, userId == null ? 0L : userId)
                .eq(IdempotencyRecordDO::getScope, scope)
                .eq(IdempotencyRecordDO::getIdempotencyKey, key);
        idempotencyRecordMapper.delete(wrapper);
        log.info("[幂等] 已释放幂等键 userId={} scope={} key={}", userId, scope, key);
    }
}
