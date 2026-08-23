package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FeedingRecordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 喂食记录Mapper
 *
 * @author FaFa Team
 * @since 2.0
 */
@Mapper
public interface FeedingRecordMapper extends BaseMapper<FeedingRecordPO> {
}
