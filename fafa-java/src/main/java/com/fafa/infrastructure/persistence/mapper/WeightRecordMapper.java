package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.WeightRecordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 体重记录Mapper
 *
 * @author FaFa Team
 * @since 2.0
 */
@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecordPO> {
}
