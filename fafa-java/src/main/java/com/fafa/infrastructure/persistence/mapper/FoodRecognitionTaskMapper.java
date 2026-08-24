package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.FoodRecognitionTaskPO;
import org.apache.ibatis.annotations.Mapper;

/** 食品识别任务 Mapper。 */
@Mapper
public interface FoodRecognitionTaskMapper extends BaseMapper<FoodRecognitionTaskPO> {
}
