package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FeedingPlanItemPO;
import org.apache.ibatis.annotations.Mapper;

/** 喂食计划项 Mapper。 */
@Mapper
public interface FeedingPlanItemMapper extends BaseMapper<FeedingPlanItemPO> {
}
