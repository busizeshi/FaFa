package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FoodTransitionPlanPO;
import org.apache.ibatis.annotations.Mapper;

/** 换粮计划 Mapper。 */
@Mapper
public interface FoodTransitionPlanMapper extends BaseMapper<FoodTransitionPlanPO> {
}
