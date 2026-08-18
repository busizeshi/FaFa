package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.ExcretionRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 排便记录Mapper
 */
@Mapper
public interface ExcretionRecordMapper extends BaseMapper<ExcretionRecordDO> {
    
    /**
     * 统计日期范围内某类型的排便次数
     */
    int countByPetIdAndTypeAndDateRange(@Param("petId") Long petId,
                                        @Param("type") String type,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
