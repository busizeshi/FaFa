package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.DailyStatusPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日状态记录Mapper
 *
 * @author FaFa Team
 * @since 2.0
 */
@Mapper
public interface DailyStatusMapper extends BaseMapper<DailyStatusPO> {
}
