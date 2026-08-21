package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（MP 自动生成 CRUD，逻辑删除自动拦截）
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
