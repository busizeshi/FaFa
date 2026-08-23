package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ReminderCompletionHistoryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提醒完成历史Mapper
 *
 * @author FaFa Team
 * @since 3.0
 */
@Mapper
public interface ReminderCompletionHistoryMapper extends BaseMapper<ReminderCompletionHistoryPO> {
}
