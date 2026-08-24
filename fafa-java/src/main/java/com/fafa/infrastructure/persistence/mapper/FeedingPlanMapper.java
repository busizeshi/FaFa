package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FeedingPlanPO;
import org.apache.ibatis.annotations.Mapper;

/** 喂食计划 Mapper。 */
@Mapper
public interface FeedingPlanMapper extends BaseMapper<FeedingPlanPO> {
}
