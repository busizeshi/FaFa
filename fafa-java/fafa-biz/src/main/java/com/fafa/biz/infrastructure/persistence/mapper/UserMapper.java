package com.fafa.biz.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.biz.infrastructure.persistence.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠主账号 Mapper（常规 CRUD 走 MyBatis-Plus；复杂查询写显式 SQL，架构规范 §8.2）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
