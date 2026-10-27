package com.fafa.biz.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.biz.infrastructure.persistence.entity.OperationJobDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异步任务 Mapper（M02 起接入业务使用）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Mapper
public interface OperationJobMapper extends BaseMapper<OperationJobDO> {
}
