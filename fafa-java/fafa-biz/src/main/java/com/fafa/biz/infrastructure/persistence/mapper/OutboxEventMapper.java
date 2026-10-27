package com.fafa.biz.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.biz.infrastructure.persistence.entity.OutboxEventDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Outbox 事件 Mapper（仅 infrastructure.mq 与业务应用服务使用）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventDO> {
}
