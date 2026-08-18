package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.PhotoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 照片Mapper
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Mapper
public interface PhotoMapper extends BaseMapper<PhotoDO> {

    /**
     * 根据宠物ID和日期范围查询照片
     */
    List<PhotoDO> selectByPetIdAndDateRange(@Param("petId") Long petId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 取消宠物的所有封面照片
     */
    void unsetAllCoversByPetId(@Param("petId") Long petId);
}
