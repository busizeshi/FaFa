package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.WeightRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 体重记录Mapper
 */
@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecordDO> {
}
