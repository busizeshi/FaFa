package com.fafa.biz.infrastructure.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MyBatis-Plus 审计字段自动填充：created_at / updated_at 统一写 UTC。
 *
 * <p>背景（架构规范 §7）：MySQL 容器时区为 Asia/Shanghai，若依赖列默认值
 * {@code DEFAULT CURRENT_TIMESTAMP} 会写入本地时间；因此统一由应用层
 * 在 INSERT/UPDATE 时填充 UTC 值，列默认值仅作为兜底。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /** 创建时间字段名 */
    private static final String FIELD_CREATED_AT = "createdAt";

    /** 更新时间字段名 */
    private static final String FIELD_UPDATED_AT = "updatedAt";

    /** {@inheritDoc}：INSERT 时填充 createdAt / updatedAt（不覆盖调用方显式传入值） */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        this.strictInsertFill(metaObject, FIELD_CREATED_AT, LocalDateTime.class, nowUtc);
        this.strictInsertFill(metaObject, FIELD_UPDATED_AT, LocalDateTime.class, nowUtc);
    }

    /** {@inheritDoc}：UPDATE 时刷新 updatedAt */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, FIELD_UPDATED_AT, LocalDateTime.class, LocalDateTime.now(ZoneOffset.UTC));
    }
}
