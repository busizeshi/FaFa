package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FoodCardPO;
import org.apache.ibatis.annotations.Mapper;

/** 食品管理卡 Mapper。 */
@Mapper
public interface FoodCardMapper extends BaseMapper<FoodCardPO> {
}
