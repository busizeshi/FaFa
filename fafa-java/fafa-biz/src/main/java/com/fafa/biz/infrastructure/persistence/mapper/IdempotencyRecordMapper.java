package com.fafa.biz.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.biz.infrastructure.persistence.entity.IdempotencyRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordDO> {
}
