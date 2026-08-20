package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Mapper
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
    
    /**
     * 查询过期的待注销用户
     * 
     * @param status 用户状态（待注销）
     * @param expirationTime 过期时间
     * @return 过期的待注销用户列表
     */
    @Select("SELECT * FROM user WHERE status = #{status} " +
            "AND deletion_requested_at IS NOT NULL " +
            "AND deletion_requested_at <= #{expirationTime} " +
            "AND deleted = 0")
    List<UserDO> findExpiredDeletionRequests(@Param("status") Integer status,
                                              @Param("expirationTime") LocalDateTime expirationTime);
}

