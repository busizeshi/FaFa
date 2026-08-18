package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.FeedRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 喂食记录 Mapper
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Mapper
public interface FeedRecordMapper extends BaseMapper<FeedRecordDO> {
    
    /**
     * 统计喂食次数
     */
    int countByPetIdAndDateRange(@Param("petId") Long petId, 
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
}
