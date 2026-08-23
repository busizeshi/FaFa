package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ReminderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提醒Mapper
 *
 * @author FaFa Team
 * @since 3.0
 */
@Mapper
public interface ReminderMapper extends BaseMapper<ReminderPO> {
}
