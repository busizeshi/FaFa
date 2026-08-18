package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.WaterRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 饮水记录Mapper
 */
@Mapper
public interface WaterRecordMapper extends BaseMapper<WaterRecordDO> {
    
    /**
     * 统计日期范围内的饮水总量
     */
    int sumAmountByPetIdAndDateRange(@Param("petId") Long petId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
}
