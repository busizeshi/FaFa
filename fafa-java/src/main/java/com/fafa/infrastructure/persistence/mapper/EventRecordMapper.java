package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.EventRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件记录Mapper
 */
@Mapper
public interface EventRecordMapper extends BaseMapper<EventRecordDO> {
}
