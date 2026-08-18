package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.ReminderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒 Mapper
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Mapper
public interface ReminderMapper extends BaseMapper<ReminderDO> {
    
    /**
     * 查询待推送的提醒
     */
    List<ReminderDO> findPendingNotifications(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计提醒数量
     */
    int countByPetId(@Param("petId") Long petId, @Param("status") String status);
    
    /**
     * 统计用户提醒数量
     */
    int countByUserId(@Param("userId") Long userId, @Param("status") String status);
}
