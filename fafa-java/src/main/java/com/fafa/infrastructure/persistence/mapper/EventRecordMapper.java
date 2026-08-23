package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.EventRecordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件记录Mapper
 *
 * @author FaFa Team
 * @since 3.0
 */
@Mapper
public interface EventRecordMapper extends BaseMapper<EventRecordPO> {
}
