package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
